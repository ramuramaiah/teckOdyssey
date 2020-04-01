package com.protobuf.examples;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.DynamicMessage.Builder;

public class DynamicSerDe {
	public static Descriptors.Descriptor getProtoBufDescriptor(File descripFile, String descripName) throws Exception {
		Descriptors.Descriptor desc = null;
		
		try {
			DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(
					Files.readAllBytes(Paths.get(descripFile.getPath())));

            for (DescriptorProtos.FileDescriptorProto fdp: descriptorSet.getFileList()) {
                Descriptors.FileDescriptor fd = Descriptors.FileDescriptor.buildFrom(fdp, new Descriptors.FileDescriptor[]{});

                for (Descriptors.Descriptor descriptor: fd.getMessageTypes()) {
                    if (descripName.equals(descriptor.getFullName()) || descripName.equals(descriptor.getName())) {
                        desc=descriptor;
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
		
		return desc;
	}
	
	// This function fills in a Person message based on user input.
	public static Builder addPersonDynamically(Descriptors.Descriptor desc) throws Exception {
		Builder peopleBuilder = null;
		
		FieldDescriptor peopleDesc = desc.findFieldByName("people");
		if (peopleDesc.getType() == FieldDescriptor.Type.MESSAGE) {
			Descriptor peopleType = peopleDesc.getMessageType();
			peopleBuilder = DynamicMessage.newBuilder(peopleType);
			{
				FieldDescriptor nameDesc = peopleType.findFieldByName("name");
				peopleBuilder.setField(nameDesc, "ramu");
				FieldDescriptor idDesc = peopleType.findFieldByName("id");
				peopleBuilder.setField(idDesc, 1);
				FieldDescriptor emailDesc = peopleType.findFieldByName("email");
				peopleBuilder.setField(emailDesc, "ramu@gmail.com");
				
				FieldDescriptor phonesDesc = peopleType.findFieldByName("phones");
				if (peopleDesc.getType() == FieldDescriptor.Type.MESSAGE) {
					Descriptor phonesType = phonesDesc.getMessageType();
					Builder phonesBuilder1 = DynamicMessage.newBuilder(phonesType);
					{
						FieldDescriptor numberDesc = phonesType.findFieldByName("number");
						phonesBuilder1.setField(numberDesc, "123");
						
						FieldDescriptor typeDesc = phonesType.findFieldByName("type");
						phonesBuilder1.setField(typeDesc, "mobile");
					}
					
					Builder phonesBuilder2 = DynamicMessage.newBuilder(phonesType);
					{
						FieldDescriptor numberDesc = phonesType.findFieldByName("number");
						phonesBuilder2.setField(numberDesc, "1234");
						
						FieldDescriptor typeDesc = phonesType.findFieldByName("type");
						phonesBuilder2.setField(typeDesc, "home");
					}
					
					Builder phonesBuilder3 = DynamicMessage.newBuilder(phonesType);
					{
						FieldDescriptor numberDesc = phonesType.findFieldByName("number");
						phonesBuilder3.setField(numberDesc, "12345");
						
						FieldDescriptor typeDesc = phonesType.findFieldByName("type");
						phonesBuilder3.setField(typeDesc, "work");
					}
					peopleBuilder.addRepeatedField(phonesDesc, phonesBuilder1.buildPartial());
					peopleBuilder.addRepeatedField(phonesDesc, phonesBuilder2.buildPartial());
					peopleBuilder.addRepeatedField(phonesDesc, phonesBuilder3.buildPartial());
				}
			}
		}
		
		return peopleBuilder;
	}
  
	// Main function:  Reads the entire address book from a file,
	//   adds one person based on user input, then writes it back out to the same
	//   file.
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage:  DynamicSerDe ADDRESS_BOOK_DESCRIPTOR_FILE ADDRESS_BOOK_FILE");
			System.exit(-1);
		}

		Descriptors.Descriptor desc = getProtoBufDescriptor(new File(args[0]), "AddressBook");
		Builder addressBookBuilder = DynamicMessage.newBuilder(desc);
		FieldDescriptor peopleDesc = desc.findFieldByName("people");
		
		Builder personBuilder = addPersonDynamically(desc);
		addressBookBuilder.addRepeatedField(peopleDesc, personBuilder.buildPartial());
		
		DynamicMessage msg=addressBookBuilder.buildPartial();

		// Write the new address book back to disk.
		FileOutputStream output = new FileOutputStream(args[1]);
		try {
			msg.writeTo(output);
		} finally {
			output.close();
		}
	}
}
