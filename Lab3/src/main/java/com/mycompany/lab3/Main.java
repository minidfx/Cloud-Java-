package com.mycompany.lab3;

import java.util.Scanner;

/**
 *
 * @author Burgy Benjamin
 */
public class Main
{
    @SuppressWarnings("empty-statement")
    public static void main(String[] args)
    {
        Cloud[] clouds = new Cloud[0];

        for (Cloud cloud : clouds)
        {
            cloud.create();
        }

        System.out.println("Press 'A' to destroy instance created.");
        Scanner scanInput = new Scanner(System.in);
        
        while(!scanInput.nextLine().equals("A"));

        for (Cloud cloud : clouds)
        {
            cloud.destroy();
        }
    }
}
