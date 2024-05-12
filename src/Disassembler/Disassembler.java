package Assignment2;

import java.io.*;
import java.util.HashMap;

public class Disassembler {
    // To run: javac Disassembler.java  ==>  java Disassembler.java assignment1copy.legv8asm.machine
    public static HashMap<Integer, String> instructionSet = new HashMap<>();
    public static HashMap<Integer, String> conditions = new HashMap<>();
    public static int labelCount = 1;

    public static void main(String[] args) {
        initializeOpcodes(); // Loads the instructions
        initializeConditions(); // Loads the condition code 

        try {
            File file = new File(args[0]); // Opens the binary file for reading
            
            DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

            while(inputStream.available() >= 4) { // Ensures there are at least 4 bytes to read
            	
                
                int instruction = inputStream.readInt(); // Read four bytes from the stream and combine into a single instruction
                disassembleInstruction(instruction); // Process and print the disassembled instruction
            }
        }
        catch (IOException e) {
            System.out.println(e); // Print any I/O errors that occur
        }
    }

    public static void disassembleInstruction(int instruction) {
        StringBuilder instructionString = new StringBuilder();

        // Extract opcode fields from instruction
        int op1 = (instruction >> 21) & 0x7FF; // R_D instructions
        int op2 = (instruction >> 22) & 0x3FF; // I instructions
        int op3 = (instruction >> 24) & 0xFF; // CB instructions
        int op4 = (instruction >> 26) & 0x3F; //B instructions

        // Determine instruction type based on opcode and process accordingly
        if (instructionSet.containsKey(op1)) {
            instructionString.append(instructionSet.get(op1));
            switch(op1) {
                case 0b10001011000: // ADD
                case 0b10001010000: // AND
                case 0b11001010000: // EOR
                case 0b10101010000: //ORR
                case 0b11001011000: //SUB
                case 0b11101011000:  //SUBS
                case 0b10011011000: //MUL
                	
                    appendRegisterNames(instruction, instructionString);
                    break;
                    
                case 0b11010110000: // BR
                    appendRegisterName(instruction, instructionString, true);
                    break;
                    
                case 0b11010011011: // LSL
                case 0b11010011010: // LSR
                    appendShiftInstruction(instruction, instructionString);
                    break;
                    
                case 0b11111111101: // PRNT
                    appendRegisterName(instruction, instructionString, false);
                    break;
                    
                case 0b11111000010: // LDUR
                case 0b11111000000: // STUR
                    appendMemoryInstruction(instruction, instructionString);
                    break;
                    
                default:
                    
                    break;
            }
            
        }
        else if (instructionSet.containsKey(op2)) {
        	
            instructionString.append(instructionSet.get(op2));
            appendALUInstruction(instruction, instructionString);
        }
        
        else if (instructionSet.containsKey(op3)) {
        	
            instructionString.append(instructionSet.get(op3));
            appendBranchInstruction(instruction, instructionString, false);
        }
        
        else if (instructionSet.containsKey(op4)) {
        	
            instructionString.append(instructionSet.get(op4));
            appendBranchInstruction(instruction, instructionString, true);
        }
        
        else {
            System.out.println("Opcode not found --> Error with program");
            return;
        }

        System.out.println("L" + labelCount + ": " + instructionString);
        labelCount++;
    }

    // Appends register names for three-register instructions
    private static void appendRegisterNames(int instruction, StringBuilder builder) {
    	
        int[] registers = {instruction & 0x1F, (instruction >> 5) & 0x1F, (instruction >> 16) & 0x1F};
        
        for (int reg : registers) {
            builder.append(" ").append(registerName(reg));
        }
    }

    // Appends register name, used for instructions with potentially one or two register names
    private static void appendRegisterName(int instruction, StringBuilder builder, boolean single) {
    	
        int Rn = (instruction >> 5) & 0x1F;
        
        builder.append(" ").append(registerName(Rn));
        
        if (!single) {
            int Rd = instruction & 0x1F;
            builder.append(", ").append(registerName(Rd));
        }
    }

    // Appends shift instructions details (register names and shift amount)
    private static void appendShiftInstruction(int instruction, StringBuilder builder) {
    	
        int Rd = instruction & 0x1F;
        int Rn = (instruction >> 5) & 0x1F;
        int shamt = (instruction >> 10) & 0x3F;          
        
        builder.append(" ").append(registerName(Rd)).append(", ").append(registerName(Rn)).append(", #").append(shamt);
    }

    // Appends memory access instructions details (register names and data/address offset)
    private static void appendMemoryInstruction(int instruction, StringBuilder builder) {
    	
        int Rt = instruction & 0x1F;
        int Rn = (instruction >> 5) & 0x1F;
        int DTAddr = (instruction >> 12) & 0x1FF;
        if (DTAddr >= 256) DTAddr -= 512;  // Adjusting for signed offset
        
        builder.append(" ").append(registerName(Rt)).append(", [").append(registerName(Rn)).append(", #").append(DTAddr).append("]");
    }

    // Appends ALU immediate instructions details (register names and immediate value)
    private static void appendALUInstruction(int instruction, StringBuilder builder) {
    	
        int Rd = instruction & 0x1F;
        int Rn = (instruction >> 5) & 0x1F;
        int ALUImm = (instruction >> 10) & 0xFFF;
        if (ALUImm >= 2048) ALUImm -= 4096;  // Adjust for sign extension
        
        builder.append(" ").append(registerName(Rd)).append(", ").append(registerName(Rn)).append(", #").append(ALUImm);
    }

    // Appends branch instructions details, including label calculations for jumps
    private static void appendBranchInstruction(int instruction, StringBuilder builder, boolean longAddress) {
    	
        int BranchAddr = (longAddress) ? (instruction & 0x3FFFFFF) : ((instruction >> 5) & 0x7FFFF);
        
        if (BranchAddr >= (longAddress ? 33554432 : 262144)) {
            BranchAddr -= (longAddress ? 67108864 : 524288);  // Two's complement
        }
        
        builder.append(" L").append(labelCount + BranchAddr);
    }

    // Converts a register code to its name, handling special registers
    private static String registerName(int regCode) {
    	
        switch (regCode) {
        
            case 28: return "SP";
            case 29: return "FP";
            case 30: return "LR";
            case 31: return "XZR";
            
            default: return "X" + regCode;
        }
    }
    
    // Initializes the opcodes
    public static void initializeOpcodes() {
		instructionSet.put(0b10001011000, "ADD");
		instructionSet.put(0b1001000100, "ADDI");
		instructionSet.put(0b10001010000, "AND");
		instructionSet.put(0b1001001000, "ANDI");
		instructionSet.put(0b000101, "B");
		instructionSet.put(0b01010100, "B.");
		instructionSet.put(0b100101, "BL");
		instructionSet.put(0b11010110000, "BR");
		instructionSet.put(0b10110101, "CBNZ");
		instructionSet.put(0b10110100, "CBZ");
		instructionSet.put(0b11001010000, "EOR");
		instructionSet.put(0b1101001000, "EORI");
		instructionSet.put(0b11111000010, "LDUR");
		instructionSet.put(0b11010011011, "LSL");
		instructionSet.put(0b11010011010, "LSR");
		instructionSet.put(0b10101010000, "ORR");
		instructionSet.put(0b1011001000, "ORRI");
		instructionSet.put(0b11111000000, "STUR");
		instructionSet.put(0b11001011000, "SUB");
		instructionSet.put(0b1101000100, "SUBI");
		instructionSet.put(0b1111000100, "SUBIS");
		instructionSet.put(0b11101011000, "SUBS");
		instructionSet.put(0b10011011000, "MUL");
		instructionSet.put(0b11111111101, "PRNT");
		instructionSet.put(0b11111111100, "PRNL");
		instructionSet.put(0b11111111110, "DUMP");
		instructionSet.put(0b11111111111, "HALT");
	}

	public static void initializeConditions() {
		conditions.put(0x0, "EQ");
		conditions.put(0x1, "NE");
		conditions.put(0x2, "HS");
		conditions.put(0x3, "LO");
		conditions.put(0x4, "MI");
		conditions.put(0x5, "PL");
		conditions.put(0x6, "VS");
		conditions.put(0x7, "VC");
		conditions.put(0x8, "HI");
		conditions.put(0x9, "LS");
		conditions.put(0xA, "GE");
		conditions.put(0xB, "LT");
		conditions.put(0xC, "GT");
		conditions.put(0xD, "LE");
		conditions.put(0xE, "");
		conditions.put(0xF, "");
	}
}