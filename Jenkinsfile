pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
    }

    environment {
        DOCKER_REGISTRY = credentials('ecr-registry-url')
        AWS_REGION = 'us-east-1'
        K8S_NAMESPACE_STAGING = 'ecommerce-staging'
        K8S_NAMESPACE_PROD = 'ecommerce-production'
        SONAR_HOST = 'http://sonarqube:9000'
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'latest'}"
        SERVICES = 'config-server,api-gateway,auth-service,user-service,product-service,inventory-service,order-service,payment-service,notification-service,search-service,ai-recommendation-service'
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_MSG = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                    env.GIT_AUTHOR = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                }
            }
        }

        stage('Build Common Library') {
            steps {
                sh 'mvn clean install -pl common-lib -DskipTests -q'
            }
        }

        stage('Build & Unit Tests') {
            steps {
                sh """
                    mvn clean compile test \
                        -pl ${SERVICES} \
                        -Dmaven.test.failure.ignore=false \
                        -T 4
                """
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/test/**'
                    )
                }
            }
        }

        stage('Integration Tests') {
            steps {
                sh """
                    mvn verify \
                        -pl auth-service,order-service,payment-service,inventory-service \
                        -Pintegration-test \
                        -Dmaven.test.failure.ignore=false
                """
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh """
                        mvn sonar:sonar \
                            -Dsonar.host.url=${SONAR_HOST} \
                            -Dsonar.projectKey=ecommerce-platform \
                            -Dsonar.projectName='E-Commerce Platform' \
                            -Dsonar.java.coveragePlugin=jacoco \
                            -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package') {
            steps {
                sh """
                    mvn package -pl ${SERVICES} \
                        -DskipTests \
                        -T 4
                """
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-ecr-credentials']]) {
                        sh "aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${DOCKER_REGISTRY}"
                    }

                    def services = [
                        'config-server',
                        'api-gateway',
                        'auth-service',
                        'user-service',
                        'product-service',
                        'inventory-service',
                        'order-service',
                        'payment-service',
                        'notification-service',
                        'search-service',
                        'ai-recommendation-service'
                    ]

                    def parallelStages = [:]

                    services.each { svc ->
                        parallelStages["Build ${svc}"] = {
                            dir(svc) {
                                sh """
                                    docker build \
                                        --build-arg JAR_FILE=target/*.jar \
                                        -t ${DOCKER_REGISTRY}/${svc}:${IMAGE_TAG} \
                                        -t ${DOCKER_REGISTRY}/${svc}:latest \
                                        .
                                    docker push ${DOCKER_REGISTRY}/${svc}:${IMAGE_TAG}
                                    docker push ${DOCKER_REGISTRY}/${svc}:latest
                                """
                            }
                        }
                    }

                    parallel parallelStages
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                        def services = [
                            'config-server',
                            'api-gateway',
                            'auth-service',
                            'user-service',
                            'product-service',
                            'inventory-service',
                            'order-service',
                            'payment-service',
                            'notification-service',
                            'search-service',
                            'ai-recommendation-service'
                        ]

                        sh "kubectl create namespace ${K8S_NAMESPACE_STAGING} --dry-run=client -o yaml | kubectl apply -f -"

                        services.each { svc ->
                            sh """
                                kubectl set image deployment/${svc} \
                                    ${svc}=${DOCKER_REGISTRY}/${svc}:${IMAGE_TAG} \
                                    -n ${K8S_NAMESPACE_STAGING} \
                                    --record || \
                                kubectl apply -f k8s/staging/${svc}.yml -n ${K8S_NAMESPACE_STAGING}

                                kubectl rollout status deployment/${svc} \
                                    -n ${K8S_NAMESPACE_STAGING} \
                                    --timeout=300s
                            """
                        }
                    }
                }
            }
        }

        stage('Smoke Tests') {
            steps {
                script {
                    def gatewayUrl = sh(
                        script: "kubectl get svc api-gateway -n ${K8S_NAMESPACE_STAGING} -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'",
                        returnStdout: true
                    ).trim()

                    def healthEndpoints = [
                        "/actuator/health",
                        "/api/auth/actuator/health",
                        "/api/users/actuator/health",
                        "/api/products/actuator/health",
                        "/api/inventory/actuator/health",
                        "/api/orders/actuator/health",
                        "/api/payments/actuator/health"
                    ]

                    healthEndpoints.each { endpoint ->
                        retry(3) {
                            sleep(time: 10, unit: 'SECONDS')
                            def response = sh(
                                script: "curl -sf -o /dev/null -w '%{http_code}' http://${gatewayUrl}:8080${endpoint}",
                                returnStdout: true
                            ).trim()
                            if (response != '200') {
                                error("Smoke test failed for ${endpoint} - HTTP ${response}")
                            }
                            echo "Health check passed: ${endpoint}"
                        }
                    }
                }
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            input {
                message 'Deploy to Production?'
                ok 'Yes, deploy to production'
                submitter 'admin,devops'
                parameters {
                    string(name: 'CONFIRM', defaultValue: '', description: 'Type "DEPLOY" to confirm')
                }
            }
            steps {
                script {
                    if (CONFIRM != 'DEPLOY') {
                        error('Production deployment not confirmed. Aborting.')
                    }

                    withCredentials([file(credentialsId: 'kubeconfig-prod', variable: 'KUBECONFIG')]) {
                        def services = [
                            'config-server',
                            'api-gateway',
                            'auth-service',
                            'user-service',
                            'product-service',
                            'inventory-service',
                            'order-service',
                            'payment-service',
                            'notification-service',
                            'search-service',
                            'ai-recommendation-service'
                        ]

                        sh "kubectl create namespace ${K8S_NAMESPACE_PROD} --dry-run=client -o yaml | kubectl apply -f -"

                        services.each { svc ->
                            sh """
                                kubectl set image deployment/${svc} \
                                    ${svc}=${DOCKER_REGISTRY}/${svc}:${IMAGE_TAG} \
                                    -n ${K8S_NAMESPACE_PROD} \
                                    --record || \
                                kubectl apply -f k8s/production/${svc}.yml -n ${K8S_NAMESPACE_PROD}

                                kubectl rollout status deployment/${svc} \
                                    -n ${K8S_NAMESPACE_PROD} \
                                    --timeout=600s
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                def msg = """
                    :white_check_mark: *Build Succeeded*
                    *Job:* ${env.JOB_NAME} #${env.BUILD_NUMBER}
                    *Branch:* ${env.BRANCH_NAME ?: 'N/A'}
                    *Commit:* ${env.GIT_COMMIT_MSG}
                    *Author:* ${env.GIT_AUTHOR}
                    *Image Tag:* ${IMAGE_TAG}
                    *Duration:* ${currentBuild.durationString}
                """.stripIndent()

                slackSend(channel: '#deployments', color: 'good', message: msg)
            }
        }

        failure {
            script {
                def msg = """
                    :x: *Build Failed*
                    *Job:* ${env.JOB_NAME} #${env.BUILD_NUMBER}
                    *Branch:* ${env.BRANCH_NAME ?: 'N/A'}
                    *Commit:* ${env.GIT_COMMIT_MSG}
                    *Author:* ${env.GIT_AUTHOR}
                    *Stage:* ${env.STAGE_NAME}
                    *Duration:* ${currentBuild.durationString}
                    *Console:* ${env.BUILD_URL}console
                """.stripIndent()

                slackSend(channel: '#deployments', color: 'danger', message: msg)

                emailext(
                    subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """
                        Build ${env.JOB_NAME} #${env.BUILD_NUMBER} failed.
                        Check console output: ${env.BUILD_URL}
                    """,
                    to: '${DEFAULT_RECIPIENTS}'
                )
            }
        }

        always {
            cleanWs()
            sh 'docker system prune -f --filter "until=24h" || true'
        }
    }
}
