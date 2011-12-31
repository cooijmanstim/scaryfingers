import java.util.*;

// weird incomplete forth dialect, unfinished, untested
public class ForthProgram extends Program {
	private final byte[] instructions;
	private final Stack stack;
	private final Stack rstack; // using bytes for instruction pointers loses information in long programs
	private byte last_output; // only store last output
	private int instruction_pointer = 0, output_pointer = 0;

	private int execution_time = 0;
	private boolean illegal = false;
	
	private int[][] vocabulary;
	private boolean in_definition = false, defining_word_known = false;
	
	public static ForthProgram fromString(String instructions, int memory_size) {
		return fromBytes(instructions.getBytes(), memory_size);
	}
	
	public static ForthProgram fromBytes(byte[] instructions, int memory_size) {
		ForthProgram fp = new ForthProgram(memory_size);
		for (byte instruction: instructions)
			fp = new ForthProgram(fp, instruction);
		return fp;
	}
	
	// construct the empty program
	public ForthProgram(int memory_size) {
		this.instructions = new byte[0];
		this.vocabulary = new int[0][2];
		this.stack = new Stack(memory_size);
		this.rstack = new Stack(memory_size);
	}

	// construct a program that is an extension of that, the only
	// difference being that it has one additional instruction
	public ForthProgram(ForthProgram that, byte additional_instruction) {
		this.last_output = that.last_output;
		this.output_pointer = that.output_pointer;
		this.execution_time = that.execution_time;
		this.illegal = that.illegal;
		
		this.stack = new Stack(that.stack.capacity());
		this.rstack = new Stack(that.rstack.capacity());
		this.vocabulary = that.vocabulary;

		this.instructions = new byte[that.instructions.length + 1];
		for (int i = 0; i < that.instructions.length; i++)
			this.instructions[i] = that.instructions[i];
		this.instructions[this.instructions.length - 1] = additional_instruction;
	}

	@Override public void step() {
		if (illegal)
			throw new RuntimeException("program has halted");

		byte instruction = instructions[instruction_pointer++];

		if (in_definition) {
			if (!defining_word_known) {
				defining_word_known = true;
				defineWord(instruction, instruction_pointer);
			}
		} else {
			switch (instruction) {
			case ':':
				in_definition = true;
				defining_word_known = false;
				break;
			case ';':
				if (in_definition) {
					in_definition = false;
					defining_word_known = false;
				} else {
					instruction_pointer = rstack.pop();
				}
				break;
			case '!':
				rstack.push((byte)instruction_pointer);
				break;
			case '.':
				last_output = stack.pop();
				output_pointer++;
				break;
			case '"': // dup
				stack.push(stack.peek());
				break;
			case '+':
				stack.push((byte)(stack.pop() + stack.pop()));
				break;
			case '-':
				stack.push((byte)(stack.pop() - stack.pop()));
				break;
			case '*':
				stack.push((byte)(stack.pop() * stack.pop()));
				break;
			case '/':
				stack.push((byte)(stack.pop() / stack.pop()));
				break;
			case '%':
				stack.push((byte)(stack.pop() % stack.pop()));
				break;
			default:
				int there = beginningOfWord(instruction);
				if (there >= 0) {
					rstack.push((byte)instruction_pointer);
					instruction_pointer = there;
				} else {
					illegal = true;
				}
				break;
			}
		}

		execution_time++;
	}
	
	private void defineWord(byte word, int instruction_pointer) {
		int[][] new_vocabulary = new int[vocabulary.length + 1][2];
		for (int i = 0; i < vocabulary.length; i++)
			for (int j = 0; j < vocabulary[i].length; j++)
				new_vocabulary[i][j] = vocabulary[i][j];
		vocabulary = new_vocabulary;

		int[] definition = vocabulary[vocabulary.length - 1];
		definition[0] = word;
		definition[1] = instruction_pointer;
	}
	
	private int beginningOfWord(byte word) {
		for (int i = vocabulary.length - 1; i >= 0; i--) {
			if (vocabulary[i][0] == word)
				return vocabulary[i][1];
		}
		return -1;
	}

	public String toString() {
		return new String(instructions);
	}
	
	@Override public int codeLength() {
		// TODO: estimate bitwise length
		return instructions.length;
	}
	
	@Override public int executionTime() {
		return execution_time;
	}
	
	@Override public int memorySize() {
		return stack.size();
	}

	@Override public int outputLength() {
		return output_pointer;
	}
	
	@Override public byte lastOutput() {
		return last_output;
	}

	@Override public boolean finished() {
		return instruction_pointer == instructions.length;
	}

	@Override public boolean illegal() {
		return illegal;
	}

	@Override public int cost() {
		// those about to output get high priority -- if they fail to be consistent
		// with the target sequence, they will be discarded and their memory freed
		if (!finished() && instructions[instruction_pointer] == '.')
			return 0;
		// having output is a good thing -- likely to get more output
		return codeLength() - output_pointer + Util.ilog2(execution_time);
	}

	@Override public ForthProgram[] successors() {
		ForthProgram[] ps = new ForthProgram[0];
		// TODO
		return ps;
	}
	
	
	private static class Stack {
		private final byte[] xs;
		private int size = 0;

		public Stack(int capacity) {
			xs = new byte[capacity];
		}
		
		public Stack(Stack that) {
			xs = that.xs.clone();
		}

		public int capacity() { return xs.length; }
		public int size() { return size; }
		
		public void push(byte x) {
			if (size == xs.length)
				// XXX: fail silently or throw or return -1?
				return;
			xs[size] = x;
			size++;
		}

		public byte pop() {
			if (size == 0)
				return -1;
			size--;
			byte x = xs[size];
			return x;
		}
		
		public byte peek() {
			if (size == 0)
				return -1;
			return xs[size - 1];
		}
	}
	

	public static void testProgram(byte[] code, int memory_size, byte[] expected_output) {
		Program program = ForthProgram.fromBytes(code, memory_size);
		byte[] actual_output = program.run();
		if (!Arrays.equals(actual_output, expected_output)) {
			System.err.println("program:  "+code);
			System.err.println("expected: "+Arrays.toString(expected_output));
			System.err.println("actual:   "+Arrays.toString(actual_output));
			if (program.illegal())
				System.err.println("program ended up in illegal state");
			System.exit(1);
		}
	}

	public static void test() {
		testProgram(new byte[]{ '\'', 'a', '.', '\'', 1, '+', '.', '\'', 1, '+', '.' }, 2,
		            new byte[]{ 'a', 'b', 'c' });
		testProgram(new byte[]{ '\'', 1, '.', '!', '"', '+', '.', ';' }, 2,
		            new byte[]{ 1, 2, 4, 8, 16, 32, 64, -128 });
	}
}
