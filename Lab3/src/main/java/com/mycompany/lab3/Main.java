package com.mycompany.lab3;

import java.util.Scanner;

/**
 *
 * @author Burgy Benjamin
 */
public class Main
{
    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws Exception
    {
        Cloud[] clouds = new Cloud[1];

        //clouds[0] = new OpenStackImpl();
        clouds[0] = new AmazonImpl();

        for (Cloud cloud : clouds)
        {
            cloud.create();
        }

        try
        {
            System.out.println("Press 'A' to destroy instance created.");
            Scanner scanInput = new Scanner(System.in);

            while (!scanInput.nextLine().equals("A"));

            for (Cloud cloud : clouds)
            {
                cloud.destroy();
            }
        }
        finally
        {
            for (Cloud cloud : clouds)
            {
                cloud.release();
            }
        }
    }
}
