import java.util.*;

public class BrainfuckProgram extends Program {
	public static final byte[] INSTRUCTIONS = new byte[]{
		'.',
		// increment/decrement value in current cell (+/- in brainfuck)
		'^', 'v',
		// binary operators (operands are current cell and cell
		// to the left, result is stored in current cell)
		'+', '-', '*', '/', '%', '|', '&', 'x',
		'<', '>', // move memory pointer
		'[', ']', // loopitypoop
		'@', // set memory pointer to value in current cell 
		'!', // set instruction pointer to value in current cell
	};

	
	private final byte[] instructions;
	private final byte[] memory;
	private byte last_output; // only store last output
	private int instruction_pointer = 0, memory_pointer = 0, output_pointer = 0;
	
	private final int[][] loop_endpoints;
	
	private int execution_time = 0;
	private boolean illegal = false;

	// construct the empty program
	public BrainfuckProgram(int memory_size) {
		instructions = new byte[0];
		loop_endpoints = new int[0][2];
		memory = new byte[memory_size];
	}
	
	// construct a program with the given sequence of instructions
	public BrainfuckProgram(byte[] instructions, int memory_size) {
		this.instructions = instructions;
		this.memory = new byte[memory_size];
		this.loop_endpoints = new int[Util.count(']', instructions)][2];
		for (int i = 0, j = 0; i < instructions.length; i++) {
			if (instructions[i] == ']') {
				loop_endpoints[j][0] = searchIndexOfMatchingBracketFor(i);
				loop_endpoints[j][1] = i;
				j++;
			}
		}
	}

	// construct a program that is an extension of that, the only
	// difference being that it has one additional instruction
	public BrainfuckProgram(BrainfuckProgram that, byte additional_instruction) {
		this.memory = that.memory.clone();
		this.memory_pointer = that.memory_pointer;

		this.last_output = that.last_output;
		this.output_pointer = that.output_pointer;
		
		this.execution_time = that.execution_time;
		this.illegal = that.illegal;

		this.instructions = new byte[that.instructions.length + 1];
		for (int i = 0; i < that.instructions.length; i++)
			this.instructions[i] = that.instructions[i];
		this.instructions[this.instructions.length - 1] = additional_instruction;
		this.instruction_pointer = that.instruction_pointer;

		if (additional_instruction == ']') {
			this.loop_endpoints = new int[that.loop_endpoints.length + 1][2];
			for (int i = 0; i < that.loop_endpoints.length; i++)
				this.loop_endpoints[i] = that.loop_endpoints[i];

			int[] loop_endpoints = this.loop_endpoints[this.loop_endpoints.length - 1];
			loop_endpoints[0] = searchIndexOfMatchingBracketFor(this.instructions.length - 1);
			loop_endpoints[1] = this.instructions.length - 1;

			// no matching opening bracket
			if (loop_endpoints[0] < 0)
				this.illegal = true;
		} else {
			this.loop_endpoints = that.loop_endpoints;
		}
	}

	@Override
	public void step() {
		if (illegal)
			throw new RuntimeException("program has halted");

		byte instruction = instructions[instruction_pointer++];
		
		//System.out.println(this + " does " + (char)instruction + " while at " + memory_pointer + " in " + Arrays.toString(memory));

		switch (instruction) {
		case '<':
			memory_pointer--;
			if (memory_pointer < 0)
				illegal = true;
			break;
		case '>':
			memory_pointer++;
			if (memory_pointer >= memory.length)
				illegal = true;
			break;
		case 'v':
			memory[memory_pointer]--;
			break;
		case '^':
			memory[memory_pointer]++;
			break;
		case '.':
			last_output = memory[memory_pointer];
			output_pointer++;
			break;
		case '[':
			if (memory[memory_pointer] == 0) {
				instruction_pointer = lookupIndexOfMatchingBracketFor(instruction_pointer - 1);
				if (instruction_pointer < 0 || instruction_pointer >= instructions.length)
					illegal = true;
			}
			break;
		case ']':
			if (memory[memory_pointer] != 0) {
				instruction_pointer = lookupIndexOfMatchingBracketFor(instruction_pointer - 1);
				if (instruction_pointer < 0 || instruction_pointer >= instructions.length)
					illegal = true;
			}
			break;
		case '@':
			memory_pointer = memory[memory_pointer];
			if (memory_pointer < 0 || memory_pointer >= memory.length)
				illegal = true;
			break;
		case '!':
			instruction_pointer = memory[memory_pointer];
			if (instruction_pointer < 0 || instruction_pointer >= instructions.length)
				illegal = true;
			break;
		case '+':
			if (memory_pointer == 0)
				illegal = true;
			else
				memory[memory_pointer] += memory[memory_pointer - 1];
			break;
		case '-':
			if (memory_pointer == 0)
				illegal = true;
			else
				memory[memory_pointer] -= memory[memory_pointer - 1];
			break;
		case '*':
			if (memory_pointer == 0)
				illegal = true;
			else
				memory[memory_pointer] *= memory[memory_pointer - 1];
			break;
		case '/':
			if (memory_pointer == 0 || memory[memory_pointer - 1] == 0)
				illegal = true;
			else
				memory[memory_pointer] /= memory[memory_pointer - 1];
			break;
		case '%':
			if (memory_pointer == 0 || memory[memory_pointer - 1] == 0)
				illegal = true;
			else
				memory[memory_pointer] %= memory[memory_pointer - 1];
			break;
		case '|':
			if (memory_pointer == 0)
				illegal = true;
			else
				memory[memory_pointer] |= memory[memory_pointer - 1];
			break;
		case '&':
			if (memory_pointer == 0)
				illegal = true;
			else
				memory[memory_pointer] &= memory[memory_pointer - 1];
			break;
		case 'x':
			if (memory_pointer == 0)
				illegal = true;
			else
				memory[memory_pointer] ^= memory[memory_pointer - 1];
			break;
		default:
			// other characters reserved
			illegal = true;
			break;
		}

		execution_time++;
	}
	
	public int lookupIndexOfMatchingBracketFor(int i) {
		for (int[] ab: loop_endpoints) {
			if (ab[0] == i) return ab[1];
			if (ab[1] == i) return ab[0];
		}
		return -1;
	}

	public int searchIndexOfMatchingBracketFor(int i) {
		int bound, direction;
		if (instructions[i] == '[') {
			bound = instructions.length - 1;
			direction = 1;
		} else {
			bound = 0;
			direction = -1;
		}
		
		int depth = 0;
		for (int j = i; direction == 1 ? j <= bound : j >= bound; j += direction) {
			if (instructions[j] == '[') depth += direction;
			if (instructions[j] == ']') depth -= direction;
			if (depth == 0)
				return j;
		}
		return -1;
	}

	@Override
	public byte[] run() {
		List<Byte> bs = new ArrayList<Byte>();
		while (!finished() && !illegal()) {
			step();
			if (bs.size() < output_pointer)
				bs.add(last_output);
		}
		byte[] sigh = new byte[bs.size()];
		int i = 0;
		for (Byte rahhhh: bs)
			sigh[i++] = rahhhh;
		return sigh;
	}

	public String toString() {
		return new String(instructions);
	}
	
	@Override
	public int codeLength() {
		// measured in bits
		return (int)Math.ceil(instructions.length * Util.ilog2(INSTRUCTIONS.length) / 8.0);
	}
	
	@Override
	public int executionTime() {
		return execution_time;
	}
	
	@Override
	public int memorySize() {
		return memory.length;
	}

	@Override
	public int outputLength() {
		return output_pointer;
	}

	@Override
	public boolean finished() {
		return instruction_pointer == instructions.length;
	}

	@Override
	public boolean illegal() {
		return illegal;
	}

	@Override
	public int cost() {
		// those about to output get high priority -- if they fail to be consistent
		// with the target sequence, they will be discarded and their memory freed
		if (!finished() && instructions[instruction_pointer] == '.')
			return 0;
		// having output is a good thing -- likely to get more output
		return codeLength() - output_pointer + Util.ilog2(execution_time);
	}

	@Override
	public boolean incrementallyConsistentWith(byte[] xs) {
		if (output_pointer == 0)
			return true;
		return last_output == xs[output_pointer - 1];
	}

	private static final byte[][] complement_pairs = {
		{ 'v', '^' }, { '<', '>' }, { '+', '-' },
		{ '*', '/' }, { 'x', 'x' }, { '[', ']' },
	};
	@Override
	public BrainfuckProgram[] successors() {
		BrainfuckProgram[] ps = new BrainfuckProgram[INSTRUCTIONS.length];
		instructions: for (int i = 0; i < INSTRUCTIONS.length; i++) {
			byte b = INSTRUCTIONS[i];

			if (instructions.length > 0) {
				// prune some pointless branches
				// (due to overflow, truncation and instruction pointer
				// teleportation, these aren't always pointless, but meh)
				byte a = instructions[instructions.length - 1];
				for (byte[] complements: complement_pairs) {
					if (b == complements[0] && a == complements[1] ||
					    b == complements[1] && a == complements[0])
						continue instructions;
				}
			}
			
			ps[i] = new BrainfuckProgram(this, b);
		}
		return ps;
	}
	

	public static void testProgram(String code, int memory_size, byte[] expected_output) {
		Program program = new BrainfuckProgram(code.getBytes(), memory_size);
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
		assert(new BrainfuckProgram(new BrainfuckProgram("12345".getBytes(), 0), (byte)']').illegal());

		testProgram("^^^^^^^^^^[>.^<v]", 2,
		            new byte[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		testProgram("^^^^^^^^^^[>^^^^^^^>^^^^^^^^^^>^^^>^<<<<v]>^^.>^.^^^^^^^..^^^.>^^.<<^^^^^^^^^^^^^^^.>.^^^.vvvvvv.vvvvvvvv.>^.>.", 5,
		            "Hello World!\n".getBytes());
		testProgram("^^>^[.*]", 2,
		            new byte[]{ 1, 2, 4, 8, 16, 32, 64, -128 });
	}
}
