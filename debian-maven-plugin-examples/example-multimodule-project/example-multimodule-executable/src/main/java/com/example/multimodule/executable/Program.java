package com.example.multimodule.executable;

import com.example.multimodule.library.Message;

public class Program {

    public static void main(String[] args) {
        Message message = new Message("hello, world");
        System.out.println(message.quoteContent());
    }

}
