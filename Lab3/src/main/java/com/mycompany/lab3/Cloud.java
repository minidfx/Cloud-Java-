package com.mycompany.lab3;

/**
 * Represents the service responsible for creating an MongoDB instance.
 *
 * @author Burgy Benjamin
 */
public interface Cloud
{
    /**
     * Creates a MongoDB instance.
     */
    void create();

    /**
     * Destroy the MongoDB instance.
     */
    void destroy();
}
