/*
Created by Maxwell Fresonke, 11/23/2014

Created for CDA3101, Project 2
Section 6692

"On my Honor, I have neither given nor received unauthorized aid in this assignment."
*/

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author max
 */
public class MIPSsim {
    //filenames
    private static final String FILENAME_DISASSEMBLY = "disassembly.txt";
    private static final String FILENAME_SIMULATION = "simulation.txt";

    //Paths
    private static final String PATH = "./";


    public static void main(String[] args) {

        /* Variables for Main */
        Simulator simulator = new Simulator();

        /* Setup File Parse Operations */

        //setup file paths
        final String inputName = args[0];
        final Path inputPath = Paths.get( PATH + inputName );

        //create scanner object and start parsing
        try {
            Scanner inputScanner = new Scanner( inputPath );
            //feeds data, line-by-line, into simulator
            while ( inputScanner.hasNextLine() ) {
                simulator.processLine( inputScanner.nextLine() );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //output disassembly
        try {
            File file = new File( FILENAME_DISASSEMBLY );
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write( simulator.getDisassemblyOutput() );
            output.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        //run simulation
        simulator.run();

        //output simulation
        try {
            File file = new File( FILENAME_SIMULATION );
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write( simulator.getSimulationOutput() );
            output.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}

class Simulator implements SimulatorCallback {
    public static final String FILENAME_DISASSEMBLY = "disassembly.txt";
    public static final String FILENAME_SIMULATION = "simulation.txt";

    public static final int IPR = 8;    //items per row (for output)
    public static final char TAB = '\t';
    public static final char NEWLINE = '\n';
    public static final char POUND = '#';
    public static final char COLON = ':';
    public static final char R = 'R';
    public static final char SPACE = ' ';
    public static final String COMMA_SPACE = ", ";

    private static final String SIMULATION_SEPARATOR = "--------------------" + NEWLINE;
    private static final String SIMULATION_CYCLE_HEADER = "Cycle:";
    private static final String[] SIMULATION_REGISTER_HEADERS = { "R00:", "R08:", "R16:", "R24:" };
    private static final String SIMULATION_REGISTER_TITLE = "Registers";
    private static final String SIMULATION_DATA_TITLE = "Data";


    /**
     * @param args the command line arguments
     */

    //MIPS Simulated Memory
    public static final int MEMORY_SPACING = 4;
    private int[] register = new int[32];
    private ArrayList<Integer> memory = new ArrayList<Integer>();


    //parsing control variables
    private int processingAddress = 128;
    private boolean isProcessingInstructions = true;

    //execution control variables.
    private int cycle = 1;              //increases no matter what
    //private int executionAddress = 128; //actually determines where we are in execution.
    private int memoryStartAddress;
    private InstructionNode start = null;
    private InstructionNode end = null;
    private InstructionNode current = null;

    //output variables
    private StringBuilder disassemblyOutput = new StringBuilder();
    private StringBuilder simulationOutput = new StringBuilder();

    /* Parsing Methods */

    /**
     * This method takes in, one line at a time, the text file and creates
     * appropriate objects to accompany them.
     * @param rawBinaryLine
     */
    public void processLine( String rawBinaryLine ) {
        //now that we have the raw binary, we need to send it to output and to the right parser functions.

        //append to disassembly
        disassemblyOutput.append( rawBinaryLine );
        disassemblyOutput.append( TAB );
        disassemblyOutput.append( processingAddress );
        disassemblyOutput.append( TAB );

        //check if we are still processing instructions, or if we are dealing with the data segment.
        if ( isProcessingInstructions ) {
            processInstructionLine( rawBinaryLine );
        }
        else {
            processDataLine( rawBinaryLine );
        }

        //finish up operations
        disassemblyOutput.append( NEWLINE );
        incrementProcessingAddress();
    }

    private void processInstructionLine( String instructionLine ) {
        //we know the first two bits represent the Category, so we
        //will go ahead and categorize based on that.

        //spit up category and the rest of the String
        String categoryBits = instructionLine.substring( 0, 2 );
        String instructionBits = instructionLine.substring( 2 );

        //get the new instruction category.
        InstructionCategory instructionCategory;
        instructionCategory = InstructionCategory.getInstructionCategory( processingAddress, categoryBits, instructionBits, this );

        //get the specific instruction type
        Instruction instruction;
        instruction = instructionCategory.getInstruction();

        //add the disassembly output of the specific instruction
        disassemblyOutput.append( instruction.toString() );

        //Check if the instruction is our "last" instruction, if so, we need to change the processing, and also note the
        // the current address.
        if ( instruction instanceof Category1.Break ) {
            isProcessingInstructions = false;
            setMemoryStartAddress();
        }

        //add the instruction to the chain.
        addInstructionToChain( instructionCategory, instruction );
    }

    private void processDataLine( String dataLine ) {

        //parse the number into an actual integer
        int actualNumber = new BigInteger( dataLine, 2 ).intValue();
        //add the integer to the appropriate place in memory
        memory.add( actualNumber );
        //add the integer to the disassembly output.
        disassemblyOutput.append( actualNumber );
    }

    /**
     * Increments the processing address to the next viable address.
     */
    private void incrementProcessingAddress() {
        processingAddress += MEMORY_SPACING;
    }

    /**
     * Sets the current start address based on the current processing address. Is expected to be called right after the
     * break instruction is detected.
     */
    private void setMemoryStartAddress() {
        memoryStartAddress = processingAddress + MEMORY_SPACING;
    }

    /* Simulation Methods */

    /**
     * Runs processed simulation!
     */
    public void run() {
        //get the system a jump start
        current = start;

        while ( current != null ) {
            //set temp variable
            InstructionNode working = current;
            //move to next instruction
            current = current.getNext();
            //run instruction
            working.run();
            //output to simulation
            appendSimulationStep( working.getAddress(), working.toString() );
            ++cycle;
        }
        //we are done!
    }

    /* Node Operations */

    /**
     * Does as title implies and adds an Instruction Object to the chain. Automatically
     * sets up linking.
     * @param instruction instruction to add to chain.
     */
    private void addInstructionToChain( InstructionCategory category, Instruction instruction ) {
        InstructionNode node = new InstructionNode( category, instruction );
        if ( start == null ) {
            start = node;
            end = node;
        }
        else {
            end.setNext( node );
            end = node;
        }
    }

    /* Output Operations */

    public String getDisassemblyOutput() {
        return disassemblyOutput.toString();
    }

    public String getSimulationOutput() {
        return simulationOutput.toString();
    }

    private void appendSimulationStep( final int executionAddress, final String instructionString ) {
        simulationOutput.append( SIMULATION_SEPARATOR );
        //Cycle:1	128	ADD R1, R0, R0
        simulationOutput.append( SIMULATION_CYCLE_HEADER );
        simulationOutput.append( cycle );
        simulationOutput.append( TAB );
        simulationOutput.append( executionAddress );
        simulationOutput.append( TAB );
        simulationOutput.append( instructionString );
        simulationOutput.append( NEWLINE );

        //blank line
        simulationOutput.append( NEWLINE );

        //print registers
        simulationOutput.append( SIMULATION_REGISTER_TITLE );
        simulationOutput.append( NEWLINE );
        for( int reg = 0, index = 0; reg != register.length; ++reg) {
            int lineCount = (reg % IPR);
            if ( lineCount == 0 ) {
                simulationOutput.append( SIMULATION_REGISTER_HEADERS[index] );
                simulationOutput.append( TAB );
                ++index;
            }
            simulationOutput.append( register[reg] );
            if( lineCount == (IPR - 1) ) {
                simulationOutput.append( NEWLINE );
            }
            else {
                simulationOutput.append( TAB );
            }
        }

        //blank line
        simulationOutput.append( NEWLINE );

        //print data
        simulationOutput.append( SIMULATION_DATA_TITLE );
        for ( int count = 0, address = memoryStartAddress; count != memory.size(); address += MEMORY_SPACING, ++count) {
            if ( count % IPR == 0 ) {
                simulationOutput.append( NEWLINE );
                simulationOutput.append( address );
                simulationOutput.append( COLON );
            }
            simulationOutput.append( TAB );
            simulationOutput.append( memory.get( count ) );
        }
        simulationOutput.append( NEWLINE );
        simulationOutput.append( NEWLINE );
    }

    /* Callback Implementations! */

    @Override
    public int getRegister( int registerNumber ) {
        return register[registerNumber];
    }

    @Override
    public void setRegister( int registerNumber, int value ) {
        register[registerNumber] = value;
    }

    @Override
    public int getMemory( int memoryAddress ) {
        int index = getMemoryIndexFromAddress( memoryAddress );
        return memory.get( index );
    }

    @Override
    public void setMemory( int memoryAddress, int value ) {
        int index = getMemoryIndexFromAddress( memoryAddress );
        memory.set( index, value );
    }

    /**
     * Returns the appropriate index for any given memory address.
     * @param memoryAddress memory address to be converted.
     * @return the index of where that particular memory address is located.
     */
    private int getMemoryIndexFromAddress( int memoryAddress ) {
        int index = memoryAddress;
        //subtract the difference from when the memory starts
        index -= memoryStartAddress;
        //divide by MEMORY_SPACING to get a raw index.
        index /= MEMORY_SPACING;

        return index;
    }

    @Override
    public void jumpTo( int instructionAddress ) {
        //we need to cycle through our linked-list until we find the right node.
        InstructionNode search = start;
        while( search.getAddress() != instructionAddress ) {
            search = search.getNext();
        }
        current = search;
    }

}
/**
 * The purpose of this class is to contain each instruction, hold meta-data about the instruction, and link every
 * instruction together.
 */
class InstructionNode {
    /* Variables */
    InstructionCategory category;
    private Instruction instruction;
    private InstructionNode next = null;

    public InstructionNode( InstructionCategory category, Instruction instruction ) {
        this.category = category;
        this.instruction = instruction;
    }

    public void setNext( InstructionNode next ) {
        this.next = next;
    }

    public InstructionNode getNext() {
        return next;
    }
    public int getAddress() {
        return category.getAddress();
    }

    public void run() {
        instruction.run();
    }

    @Override
    public String toString() {
        return instruction.toString();
    }
}

interface SimulatorCallback {
    public int getRegister( int registerNumber );
    public void setRegister( int registerNumber, int value );

    public int getMemory( int memoryAddress );
    public void setMemory( int memoryAddress, int value );

    public void jumpTo( int instructionAddress );
}

/** This is the logical separation of different parsing functions for
 * Category1 Instructions.
 */
abstract class InstructionCategory {

    /* Constants */

    //Category Instructions Sorting
    protected static final String C_1 = "00";
    protected static final String C_2 = "01";
    protected static final String C_3 = "10";

    /**
     * In this method, we are sorting the instruction category based on the first
     * two bits of thw raw bits, and returning the appropriate type.
     *
     * @param categoryBits
     * @return
     */
    static InstructionCategory getInstructionCategory( int address, String categoryBits, String instructionBits, SimulatorCallback callback ) {
        //now we do a bunch of if statements to return the right object.
        //category 1
        if ( categoryBits.equals( C_1 ) ) {
            return new Category1( address, instructionBits, callback );
        }
        //category 2
        else if ( categoryBits.equals( C_2 ) ) {
            return new Category2( address, instructionBits, callback );
        }
        //category 3. Default Category
        else {//if ( categoryBits.equals( C_3 ) )
            return new Category3( address, instructionBits, callback );
        }
    }


    /* Variables */
    private String instructionBits;
    private SimulatorCallback callback;

    public int getAddress() {
        return address;
    }

    private int address;

    /**
     * This takes in the instruction and does the required operations.
     * @param instructionBits Every bit except the category bits.
     * @param callback Keeps communication with the simulator. It's how it executes its instructions.
     */
    public InstructionCategory( int address, String instructionBits, SimulatorCallback callback ) {
        this.address = address;
        this.instructionBits = instructionBits;
        this.callback = callback;
    }
    public abstract Instruction getInstruction();

    protected SimulatorCallback getCallback() {
        return callback;
    }

    protected String getInstructionBits() {
        return instructionBits;
    }
}

/** This provides the generic code for every type of instruction within a specified
 * instruction category.
 */
abstract class Instruction {

    public Instruction (  ) {
    }

    //returns the string representation, e.g. "ADD R1, R0, R0" of the current instruction.
    public abstract String toString();
    static protected String buildToString( String... args ) {
        StringBuilder output = new StringBuilder( args[0] );
        if ( args.length == 1 ) {
            return output.toString();
        }
        output.append( Simulator.SPACE );
        output.append( args[1] );
        if ( args.length == 2 ) {
            return output.toString();
        }

        for ( int a = 2; a != args.length; ++a ) {
            output.append( Simulator.COMMA_SPACE );
            output.append( args[a] );
        }
        return output.toString();
    }
    static private String addCharToNumber( final int register, final char letter ) {
        StringBuilder builder = new StringBuilder();
        builder.append( letter );
        builder.append( register );
        return builder.toString();
    }
    static protected String registerToString( final int register ) {
        return addCharToNumber( register, 'R' );
    }
    static protected String immediateToString( final int immediate ) {
        return addCharToNumber( immediate, '#' );
    }
    public abstract void run();

}

abstract class CompareInstruction extends Instruction {
    private int destinationRegister;
    private SimulatorCallback callback;

    public CompareInstruction( int destinationRegister, SimulatorCallback callback ) {
        this.callback = callback;
        this.destinationRegister = destinationRegister;
    }

    protected abstract String getValue1BinaryString();
    protected abstract String getValue2BinaryString();

    @Override
    public void run() {
        String value1BinaryString = getValue1BinaryString();
        String value2BinaryString = getValue2BinaryString();

        final int value1Length = value1BinaryString.length();
        final int value2Length = value2BinaryString.length();
        StringBuilder builder = new StringBuilder();

        //the idea of this method is to have the strings be compared, starting with their least significant bit,
        // towards the most significant bit, until one of them runs out of length!
        for( int a = 0; a != 32; ++a ) {
            int index1 = ( value1Length - ( a + 1 ) );
            int index2 = ( value2Length - ( a + 1 ) );
            char value1Char;
            char value2Char;
            //since we are going to create a 32 bit register regardless we need to be sure our index is in range.
            // If not, we are going to assume its padded with zeros.
            if ( index1 < 0 ) value1Char = '0';
            else value1Char = value1BinaryString.charAt( index1 );
            if ( index2 < 0 ) value2Char = '0';
            else value2Char = value2BinaryString.charAt( index2 );
            //turn the chars into boolean!
            boolean value1Boolean;
            boolean value2Boolean;
            boolean booleanOutput;
            if ( value1Char == '1' ) value1Boolean = true;
            else value1Boolean = false;
            if ( value2Char == '1' ) value2Boolean = true;
            else value2Boolean = false;
            //now we FINALLY have two booleans that represent the current characters!
            //now we can COMPARE THEM! YAY!
            booleanOutput = compare( value1Boolean, value2Boolean );
            //now we finally take that booleanOutput and put it back into the builder!
            if( booleanOutput ) {
                builder.insert( 0, "1" );
            }
            else {
                builder.insert( 0, "0" );
            }
        }
        //holy guacamole we are done!
        //now all we need to do is turn that builder back into an integer. Not so bad.
        int result = new BigInteger(builder.toString(), 2 ).intValue();
        //and nowwwww, put the result back into the registers!
        callback.setRegister( destinationRegister, result );
        //hallelujah!
    }

    protected abstract boolean compare( boolean value1, boolean value2 );
}

/* Real Category Classes Begin Here */

class Category1 extends InstructionCategory {
    private static final String OUTPUT_JUMP = "J";
    private static final String OUTPUT_BEQ = "BEQ";
    private static final String OUTPUT_BGTZ = "BGTZ";
    private static final String OUTPUT_BREAK = "BREAK";
    private static final String OUTPUT_SW = "SW";
    private static final String OUTPUT_LW = "LW";

    private static final int OPCODE_JUMP = 0;
    private static final int OPCODE_BEQ = 2;
    private static final int OPCODE_BGTZ = 4;
    private static final int OPCODE_BREAK = 5;
    private static final int OPCODE_SW = 6;
    private static final int OPCODE_LW = 7;

    private static final int[] OPCODE_LOC = { 0, 4 };
    private static final int[] RS_LOC = { 0, 5 };
    private static final int[] RT_LOC = { 5, 10 };
    private static final int OFFSET_LOC = 10;

    public Category1( int address, String instructionBits, SimulatorCallback callback ) {
        super( address, instructionBits, callback );
    }

    @Override
    public Instruction getInstruction() {
        //get raw string representing opcode
        String opcodeString = getInstructionBits().substring( OPCODE_LOC[0], OPCODE_LOC[1] );
        String bitsWithoutOpcode = getInstructionBits().substring( OPCODE_LOC[1] );
        int opcode = Integer.parseInt( opcodeString, 2 );
        Instruction instruction = null;

        switch( opcode ) {
            case OPCODE_JUMP:
                instruction = new UnconditionalJump( bitsWithoutOpcode );
                break;
            case OPCODE_BEQ:
                instruction = new BranchIfEqual( bitsWithoutOpcode );
                break;
            case OPCODE_BGTZ:
                instruction = new BranchIfGreaterThanZero( bitsWithoutOpcode );
                break;
            case OPCODE_BREAK:
                instruction = new Break();
                break;
            case OPCODE_SW:
                instruction = new StoreWord( bitsWithoutOpcode );
                break;
            case OPCODE_LW:
                instruction = new LoadWord( bitsWithoutOpcode );
                break;
        }
        return instruction;
    }

    /* Class Templates */

    abstract class Category1Instruction extends Instruction {
        private String bitsWithoutOpcode;

        protected String getBitsWithoutOpcode() {
            return bitsWithoutOpcode;
        }

        public Category1Instruction( String bitsWithoutOpcode ) {
            this.bitsWithoutOpcode = bitsWithoutOpcode;
        }
    }

    abstract class Branch extends Category1Instruction {

        private int offset;
        private int jumpAddress;

        public Branch( String bitsWithoutOpcode ) {
            super( bitsWithoutOpcode );
            String offsetString = bitsWithoutOpcode.substring( OFFSET_LOC );

            //assign variables
            offset = new BigInteger( offsetString, 2 ).intValue();
            offset <<= 2; //shift our offset by two, due to MIPS convention
            jumpAddress = offset;
            //add to PC + 4 to make real offset
            jumpAddress += ( getAddress() + Simulator.MEMORY_SPACING );
        }

        protected int getOffset() {
            return offset;
        }

        protected int getJumpAddress() {
            return jumpAddress;
        }
    }

    abstract class Word extends Category1Instruction {

        private int register;
        private int baseRegister;
        private int offset;

        public Word( String bitsWithoutOpcode ) {
            super( bitsWithoutOpcode );
            String baseRegisterString = getBitsWithoutOpcode().substring( RS_LOC[0], RS_LOC[1] );
            String registerString = getBitsWithoutOpcode().substring( RT_LOC[0], RT_LOC[1] );
            String offsetString = getBitsWithoutOpcode().substring( OFFSET_LOC );

            baseRegister = Integer.parseInt( baseRegisterString, 2 );
            register = Integer.parseInt( registerString, 2 );
            offset = new BigInteger( offsetString, 2 ).intValue();
        }

        protected String buildToString( final String name ) {
            StringBuilder rightSide = new StringBuilder();
            rightSide.append( offset );
            rightSide.append( '(' );
            rightSide.append( registerToString( baseRegister ) );
            rightSide.append( ')' );
            return buildToString( name, registerToString( register ), rightSide.toString() );
        }

        protected int getRegister() {
            return register;
        }

        protected int getBaseRegister() {
            return baseRegister;
        }

        protected int getOffset() {
            return offset;
        }
    }

    /* Class Implementations */

    class UnconditionalJump extends Category1Instruction {
        private int jumpLocation;

        public UnconditionalJump( String bitsWithoutOpcode ) {
            super( bitsWithoutOpcode );
            calculateJumpLocation();
        }

        /**
         * This method calcualtes the actual int of where the jump will go.
         */
        private void calculateJumpLocation() {
            StringBuilder workingString = new StringBuilder( getBitsWithoutOpcode() );
            // "shift left" two by adding two zeros on the end.
            workingString.append( "00" ); //we are now at 28 bits
            StringBuilder binaryAddressString = new StringBuilder( Integer.toBinaryString( getAddress() + Simulator.MEMORY_SPACING ) );
            //now we expand the binary address string until it is 32 bits!
            while ( workingString.length() != 32 ) {
                //insert a zero at the beginning
                workingString.insert( 0, '0' );
            }
            String upperFourBits = workingString.substring( 0, 3 );
            //add those upper four bits to the workingString
            workingString.insert( 0, upperFourBits );
            //Woohoo! We have our address! Now lets set the class variable.
            jumpLocation = new BigInteger( workingString.toString(), 2 ).intValue();
        }

        @Override
        public String toString() {
            return buildToString( OUTPUT_JUMP, immediateToString( jumpLocation ) );
        }

        @Override
        public void run() {
            getCallback().jumpTo( jumpLocation );
        }
    }

    class BranchIfEqual extends Branch {
        private int register1;
        private int register2;

        public BranchIfEqual( String bitsWithoutOpcode ) {
            //feels the offset string to the superconstructor
            super( bitsWithoutOpcode );
            String register1BinaryString = bitsWithoutOpcode.substring( RS_LOC[0], RS_LOC[1] );
            String register2BinaryString = bitsWithoutOpcode.substring( RT_LOC[0], RT_LOC[1] );
            //parse
            register1 = Integer.parseInt( register1BinaryString, 2 );
            register2 = Integer.parseInt( register2BinaryString, 2 );
        }

        @Override
        public String toString() {
            return buildToString( OUTPUT_BEQ, registerToString( register1 ), registerToString( register2 ), immediateToString( getOffset() ) );
        }

        @Override
        public void run() {
            //get values of register1 and register2
            int register1Val = getCallback().getRegister( register1 );
            int register2Val = getCallback().getRegister( register2 );

            //if they are equal, jump!
            if( register1Val == register2Val ) {
                getCallback().jumpTo( getJumpAddress() );
            }
        }
    }

    class BranchIfGreaterThanZero extends Branch {
        private int register;

        public BranchIfGreaterThanZero( String bitsWithoutOpcode ) {
            super( bitsWithoutOpcode );
            register = Integer.parseInt( getBitsWithoutOpcode().substring( RS_LOC[0], RS_LOC[1] ), 2 );
        }

        @Override
        public String toString() {
            return buildToString( OUTPUT_BGTZ, registerToString( register ), immediateToString( getOffset() ) );
        }

        @Override
        public void run() {
            int value = getCallback().getRegister( register );
            if ( value > 0 ) {
                getCallback().jumpTo( getJumpAddress() );
            }
        }
    }

    class Break extends Instruction {

        @Override
        public String toString() {
            return OUTPUT_BREAK;
        }

        @Override
        public void run() { /* does not really do anything... */ }
    }

    class StoreWord extends Word {

        public StoreWord( String bitsWithoutOpcode ) {
            super( bitsWithoutOpcode );
        }

        @Override
        public String toString() {
            return buildToString( OUTPUT_SW );
        }

        @Override
        public void run() {
            //get values
            int rtValue = getCallback().getRegister( getRegister() );
            int baseValue = getCallback().getRegister( getBaseRegister() );
            int address = ( baseValue + getOffset() );
            getCallback().setMemory( address, rtValue );
        }
    }

    class LoadWord extends Word {

        public LoadWord( String bitsWithoutOpcode ) {
            super( bitsWithoutOpcode );
        }

        @Override
        public String toString() {
            return buildToString( OUTPUT_LW );
        }

        @Override
        public void run() {
            //get values
            int baseValue = getCallback().getRegister( getBaseRegister() );
            int address = ( baseValue + getOffset() );
            int saveValue = getCallback().getMemory( address );
            getCallback().setRegister( getRegister(), saveValue );
        }

    }

}

class Category2 extends InstructionCategory {

    private static final String TITLE_ADD = "ADD";
    private static final String TITLE_SUB = "SUB";
    private static final String TITLE_MUL = "MUL";
    private static final String TITLE_AND = "AND";
    private static final String TITLE_OR = "OR";
    private static final String TITLE_XOR = "XOR";
    private static final String TITLE_NOR = "NOR";

    private static final int[] RS_LOC = { 0, 5 };
    private static final int[] RT_LOC = { 5, 10 };
    private static final int[] OPCODE_LOC = { 10, 14 };
    private static final int[] RD_LOC = { 14, 19 };

    private String rsString;
    private String rtString;
    private String rdString;

    private int source1; //source register 1
    private int source2; //source register 2
    private int destination; //destination register

    /**
     * This takes in the instruction and does the required operations.
     *
     * @param instructionBits Essentially every bit except the category bits.
     */
    public Category2( int address, String instructionBits, SimulatorCallback callback ) {
        super( address, instructionBits, callback );

        //first thing's first. We need to extract all of the Strings out of the instructionBits!
        rsString = getInstructionBits().substring( RS_LOC[0], RS_LOC[1] );
        rtString = getInstructionBits().substring( RT_LOC[0], RT_LOC[1] );
        rdString = getInstructionBits().substring( RD_LOC[0], RD_LOC[1] );

        //now we need to parse those instruction bits into usable integers, assigning accordingly.
        source1 = Integer.parseUnsignedInt( rsString, 2 );
        source2 = Integer.parseUnsignedInt( rtString, 2 );
        destination = Integer.parseUnsignedInt( rdString, 2 );
    }

    @Override
    public Instruction getInstruction() {
        //parse the opcode
        String opcodeString = getInstructionBits().substring( OPCODE_LOC[0], OPCODE_LOC[1] );
        int opcode = Integer.parseUnsignedInt( opcodeString, 2 );

        //turn the opcode into a usable Object.
        Instruction instruction = null;
        switch ( opcode ) {
            case 0: //ADD
                instruction = new Add();
                break;
            case 1: //SUB
                instruction = new Subtract();
                break;
            case 2: //MUL
                instruction = new Multiply();
                break;
            case 3: //AND
                instruction = new And();
                break;
            case 4: //OR
                instruction = new Or();
                break;
            case 5: //XOR
                instruction = new Xor();
                break;
            case 6: //NOR
                instruction = new Nor();
                break;
        }
        return instruction;
    }

    private abstract class TwoCompareInstruction extends CompareInstruction {

        public TwoCompareInstruction() {
            super( destination, getCallback() );
        }

        @Override
        protected String getValue1BinaryString() {
            return Integer.toString( getCallback().getRegister( source1 ), 2 );
        }

        @Override
        protected String getValue2BinaryString() {
            return Integer.toString( getCallback().getRegister( source2 ), 2 );
        }

        protected String buildToString( String title ) {
            return buildToString( title, registerToString( destination ), registerToString( source1 ), registerToString( source2 ) );
        }
    }

    private abstract class ArithmeticInstruction extends Instruction {
        @Override
        public void run() {
            int value1 = getCallback().getRegister( source1 );
            int value2 = getCallback().getRegister( source2 );
            int result = performOperation( value1, value2 );
            getCallback().setRegister( destination, result );
        }

        protected abstract int performOperation( final int value1, final int value2 );

        protected String buildToString( String title ) {
            return buildToString( title, registerToString( destination ), registerToString( source1 ), registerToString( source2 ) );
        }
    }

    /* Instruction Implementations */

    private class Add extends ArithmeticInstruction {

        @Override
        protected int performOperation( int value1, int value2 ) {
            return ( value1 + value2 );
        }

        @Override
        public String toString() {
            return buildToString( TITLE_ADD );
        }
    }

    private class Subtract extends ArithmeticInstruction {

        @Override
        protected int performOperation( int value1, int value2 ) {
            return ( value1 - value2 );
        }

        @Override
        public String toString() {
            return buildToString( TITLE_SUB );
        }
    }

    private class Multiply extends ArithmeticInstruction {

        @Override
        protected int performOperation( int value1, int value2 ) {
            return ( value1 * value2 );
        }

        @Override
        public String toString() {
            return buildToString( TITLE_MUL );
        }
    }

    private class And extends TwoCompareInstruction {

        @Override
        protected boolean compare( boolean value1, boolean value2 ) {
            return ( value1 && value2 );
        }

        @Override
        public String toString() {
            return buildToString( TITLE_AND );
        }
    }

    private class Or extends TwoCompareInstruction {

        @Override
        protected boolean compare( boolean value1, boolean value2 ) {
            return value1 || value2;
        }

        @Override
        public String toString() {
            return buildToString( TITLE_OR );
        }
    }

    private class Xor extends TwoCompareInstruction {

        @Override
        protected boolean compare( boolean value1, boolean value2 ) {
            if ( value1 && !value2 ) return true;
            if ( !value1 && value2 ) return true;
            return false;
        }

        @Override
        public String toString() {
            return buildToString( TITLE_XOR );
        }
    }

    private class Nor extends TwoCompareInstruction {

        @Override
        protected boolean compare( boolean value1, boolean value2 ) {
            return ( !value1 && !value2 );
        }

        @Override
        public String toString() {
            return buildToString( TITLE_NOR );
        }
    }


}

class Category3 extends InstructionCategory {
    private static final String OUTPUT_ADDI = "ADDI";
    private static final String OUTPUT_ANDI = "ANDI";
    private static final String OUTPUT_ORI = "ORI";
    private static final String OUTPUT_XORI = "XORI";

    private static final int OPCODE_ADDI = 0;
    private static final int OPCODE_ANDI = 1;
    private static final int OPCODE_ORI = 2;
    private static final int OPCODE_XORI = 3;

    private static final int[] RS_LOC = { 0, 5 };
    private static final int[] RT_LOC = { 5, 10 };
    private static final int[] OPCODE_LOC = { 10, 14 };
    private static final int[] IMMEDIATE_LOC = { 14, 30 };

    private int sourceRegister;
    private int destinationRegister;
    private String immediateString;

    /**
     * This takes in the instruction and does the required operations.
     *
     * @param instructionBits Essentially every bit except the category bits.
     */
    public Category3( int address, String instructionBits, SimulatorCallback callback ) {
        super( address, instructionBits, callback );
    }

    @Override
    public Instruction getInstruction() {
        //parse everything.
        String opcodeString;
        String destinationRegisterString;
        String sourceRegisterString;

        opcodeString = getInstructionBits().substring( OPCODE_LOC[0], OPCODE_LOC[1] );
        sourceRegisterString = getInstructionBits().substring( RS_LOC[0], RS_LOC[1] );
        destinationRegisterString = getInstructionBits().substring( RT_LOC[0], RT_LOC[1] );
        immediateString = getInstructionBits().substring( IMMEDIATE_LOC[0], IMMEDIATE_LOC[1] );

        int opcode = Integer.parseInt( opcodeString, 2 );
        sourceRegister = Integer.parseInt( sourceRegisterString, 2 );
        destinationRegister = Integer.parseInt( destinationRegisterString, 2 );

        Instruction instruction = null;
        //switch on the opcode to get the right object.
        switch ( opcode ) {
            case OPCODE_ADDI:
                instruction = new AddImmediate();
                break;
            case OPCODE_ANDI:
                instruction = new AndImmediate();
                break;
            case OPCODE_ORI:
                instruction = new OrImmediate();
                break;
            case OPCODE_XORI:
                instruction = new XOrImmediate();
                break;
        }

        return instruction;
    }

    /* Templates */

    abstract class Category3CompareInstruction extends CompareInstruction {

        private int immediateInt;

        public Category3CompareInstruction() {
            super( destinationRegister, getCallback() );
            calculateImmediate();
        }
        void calculateImmediate() {
            immediateInt = new BigInteger( immediateString, 2 ).intValue();
        }

        @Override
        protected String getValue1BinaryString() {
            return Integer.toString( getCallback().getRegister( sourceRegister ), 2 );
        }

        @Override
        protected String getValue2BinaryString() {
            return immediateString;
        }

        protected String buildToString( final String name ) {
            return buildToString( name, registerToString( destinationRegister ), registerToString( sourceRegister ), immediateToString( immediateInt ) );
        }
    }

    /* Instructions */

    class AddImmediate extends Instruction {

        private int immediateInt;
        AddImmediate () {
            calculateImmediate();
        }

        void calculateImmediate() {
            immediateInt = new BigInteger( immediateString, 2 ).intValue();
        }

        @Override
        public String toString() {
            return buildToString( OUTPUT_ADDI, registerToString( destinationRegister ), registerToString( sourceRegister ), immediateToString( immediateInt ) );
        }

        @Override
        public void run() {
            int sourceRegisterValue = getCallback().getRegister( sourceRegister );
            int result = sourceRegisterValue + immediateInt;
            getCallback().setRegister( destinationRegister, result );
        }
    }

    class AndImmediate extends Category3CompareInstruction {

        @Override
        protected boolean compare( boolean value1, boolean value2 ) {
            return value1 && value2;
        }

        @Override
        public String toString() {
            return buildToString( OUTPUT_ANDI );
        }
    }

    class OrImmediate extends Category3CompareInstruction {

        @Override
        protected boolean compare( boolean value1, boolean value2 ) {
            return value1 || value2;
        }

        @Override
        public String toString() {
            return buildToString( OUTPUT_ORI );
        }
    }

    class XOrImmediate extends Category3CompareInstruction {

        @Override
        protected boolean compare( boolean value1, boolean value2 ) {
            return ( value1 && !value2 ) || ( !value1 && value2 );
        }

        @Override
        public String toString() {
            return buildToString( OUTPUT_XORI );
        }
    }

}

