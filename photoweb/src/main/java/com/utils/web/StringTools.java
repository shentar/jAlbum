package com.utils.web;

import java.util.Random;

public class StringTools
{
    private static final String BASE = "abcdefghijklmnopqrstuvwxyz" + "0123456789"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int BASE_LENGTH = BASE.length();

    private static final Object lock = new Object();

    private static Random random = new Random();

    public static String getRandomString(int length)
    {
        if (length < 0)
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++)
        {
            int number;

            synchronized (lock)
            {
                number = random.nextInt(BASE_LENGTH);
            }

            sb.append(BASE.charAt(number));
        }

        return sb.toString();
    }
}
