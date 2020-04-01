package com.protobuf.examples;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.protobuf.examples.AddressBookSerDe.AddressBook;
import com.protobuf.examples.AddressBookSerDe.Person;

public class AddressBookApp {
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
