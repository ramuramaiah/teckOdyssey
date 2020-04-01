package com.protobuf.examples;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.protobuf.examples.AddressBookSerDe.AddressBook;
import com.protobuf.examples.AddressBookSerDe.Person;

public class AddressBookApp {
	  // This function fills in a Person message based on user input.
	  static Person PromptForAddress(BufferedReader stdin,
	                                 PrintStream stdout) throws IOException {
	    Person.Builder person = Person.newBuilder();

	    stdout.print("Enter person ID: ");
	    person.setId(Integer.valueOf(stdin.readLine()));

	    stdout.print("Enter name: ");
	    person.setName(stdin.readLine());

	    stdout.print("Enter email address (blank for none): ");
	    String email = stdin.readLine();
	    if (email.length() > 0) {
	      person.setEmail(email);
	    }

	    while (true) {
	      stdout.print("Enter a phone number (or leave blank to finish): ");
	      String number = stdin.readLine();
	      if (number.length() == 0) {
	        break;
	      }

	      Person.PhoneNumber.Builder phoneNumber =
	        Person.PhoneNumber.newBuilder().setNumber(number);

	      stdout.print("Is this a mobile, home, or work phone? ");
	      String type = stdin.readLine();
	      if (type.equals("mobile")) {
	        phoneNumber.setType("mobile");
	      } else if (type.equals("home")) {
	        phoneNumber.setType("home");
	      } else if (type.equals("work")) {
	        phoneNumber.setType("work");
	      } else {
	        stdout.println("Unknown phone type.  Using default.");
	      }

	      person.addPhones(phoneNumber);
	    }

	    return person.build();
	  }
	  
	  // Main function:  Reads the entire address book from a file,
	  //   adds one person based on user input, then writes it back out to the same
	  //   file.
	  public static void main(String[] args) throws Exception {
	    if (args.length != 1) {
	      System.err.println("Usage:  AddressBookApp ADDRESS_BOOK_FILE");
	      System.exit(-1);
	    }

	    AddressBook.Builder addressBook = AddressBook.newBuilder();

	    // Read the existing address book.
	    try {
	      FileInputStream input = new FileInputStream(args[0]);
	      try {
	        addressBook.mergeFrom(input);
	      } finally {
	        try { input.close(); } catch (Throwable ignore) {}
	      }
	    } catch (FileNotFoundException e) {
	      System.out.println(args[0] + ": File not found.  Creating a new file.");
	    }

	    Person.Builder person = Person.newBuilder();
	    person.setId(1);
	    person.setName("ramu");
	    person.setEmail("ramu@gmail.com");

	    {
	    	Person.PhoneNumber.Builder phoneNumber = Person.PhoneNumber.newBuilder();
	    	phoneNumber.setNumber("123");
	    	phoneNumber.setType("mobile");
	    	person.addPhones(phoneNumber);
	    }
	    
	    {
	    	Person.PhoneNumber.Builder phoneNumber = Person.PhoneNumber.newBuilder();
	    	phoneNumber.setNumber("1234");
	    	phoneNumber.setType("home");
	    	person.addPhones(phoneNumber);
	    }
	    
	    {
	    	Person.PhoneNumber.Builder phoneNumber = Person.PhoneNumber.newBuilder();
	    	phoneNumber.setNumber("12345");
	    	phoneNumber.setType("work");
	    	person.addPhones(phoneNumber);
	    }
	    
	    // Add an address.
	    addressBook.addPeople(person.build());

	    // Write the new address book back to disk.
	    FileOutputStream output = new FileOutputStream(args[0]);
	    try {
	      addressBook.build().writeTo(output);
	    } finally {
	      output.close();
	    }
	  }	  
}
