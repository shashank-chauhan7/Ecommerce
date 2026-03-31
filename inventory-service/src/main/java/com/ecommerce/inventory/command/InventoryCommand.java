package com.ecommerce.inventory.command;

public interface InventoryCommand {

    void execute();

    void undo();
}
