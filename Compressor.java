import java.util.*;

public class Compressor {
	private static final int MAX_ACTIVE_PROGRAM_COUNT = 1000000;

	// find (if exists) a program of length at most as many bytes as
	// the input, that has xs as a prefix of its output, and that
	// outputs this prefix within 2^xs.length execution steps.
	public Program compress(byte[] xs) {
		return compress(xs, 0, xs.length);
	}

	public Program compress(byte[] xs, int a, int b) {
		PriorityQueue<Program> to_be_extrapolated = new PriorityQueue<Program>(MAX_ACTIVE_PROGRAM_COUNT);
		PriorityQueue<Program> ps = new PriorityQueue<Program>(MAX_ACTIVE_PROGRAM_COUNT);

		try {
			// start with the empty program
			// (I don't think a better-than-nothing decompressor
			// should need more memory than the length of the
			// sequence it represents, but I haven't proven it)
			ps.add(new Program(b-a));

			while (!ps.isEmpty()) {
				Program p = ps.remove();

				// this check is only needed because the empty
				// program is initially finished
				if (!p.finished())
					p.step();

				if (p.incrementallyConsistentWith(xs)) {
					if (p.outputLength() == xs.length) {
						// this program has xs as a prefix of its output,
						// so it is a decompressor if time-limited
						return p;
						// TODO return p.freshTimeLimitedCopy();
					} else if (!p.illegal() && !p.runaway(xs.length)) {
						// this program's output so far is a prefix of xs

						if (!p.finished()) {
							// put it in the queue for another round
							ps.add(p);
						} else {
							to_be_extrapolated.add(p);
						}
					}
				}
	
				// maybe add more to the fringe
				Program r;
				while (ps.size() < MAX_ACTIVE_PROGRAM_COUNT - Program.INSTRUCTIONS.length
						&& (r = to_be_extrapolated.poll()) != null) {
					for (Program q: r.successors()) {
						if (q == null || q.illegal()) continue;

						if (q.length() <= xs.length)
							ps.add(q);
					}
				}
			}
		} catch (OutOfMemoryError e) {
			System.err.println(ps.size()+" programs active");
			System.err.println(to_be_extrapolated.size()+" programs to be extrapolated");
			Program p; int k = 0;
			while ((p = ps.poll()) != null && k++ < 50) {
				System.err.println("t:"+p.execution_time+"\t$:"+p.cost()+"\t"+p);
			}
			ps = null;
			e.printStackTrace();
		}
		
		return null;
	}

	// represents programs in a brainfuck-inspired language
	public static class Program implements Comparable<Program> {
		public static final byte[] INSTRUCTIONS = new byte[]{
			'.',
			// increment/decrement value in current cell (+/- in brainfuck)
			'^', 'v',
			// binary operators (operands are current cell and cell
			// to the left, result is stored in current cell)
			'+', '-', '*', '/', '%', '|', '&', 'x',
			//'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', // constants
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
		public Program(int memory_size) {
			instructions = new byte[0];
			loop_endpoints = new int[0][2];
			memory = new byte[memory_size];
		}
		
		// construct a program with the given sequence of instructions
		public Program(byte[] instructions, int memory_size) {
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
		public Program(Program that, byte additional_instruction) {
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
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
				memory[memory_pointer] = (byte)(instruction - '0');
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

		// step until finished or illegal, collecting output
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
		
		public int length() {
			// measured in bits
			return (int)Math.ceil(instructions.length * Util.ilog2(INSTRUCTIONS.length) / 8.0);
		}

		// returns the set of programs that are one instruction
		// longer than this, but are equal to this in every other
		// respect (i.e. execution state and everything).
		private static final byte[][] complement_pairs = {
			{ 'v', '^' }, { '<', '>' }, { '+', '-' },
			{ '*', '/' }, { 'x', 'x' }, { '[', ']' },
		};
		public Program[] successors() {
			Program[] ps = new Program[INSTRUCTIONS.length];
			instructions: for (int i = 0; i < INSTRUCTIONS.length; i++) {
				byte b = INSTRUCTIONS[i];

				if (instructions.length > 0) {
					// prune some pointless branches
					// (due to overflow, truncation and instruction pointer
					// teleportation, these aren't always pointless, but meh)
					byte a = instructions[instructions.length - 1];
					if (isConstant(a) && isConstant(b))
						continue;
					for (byte[] complements: complement_pairs) {
						if (b == complements[0] && a == complements[1] ||
						    b == complements[1] && a == complements[0])
							continue instructions;
					}
				}
				
				ps[i] = new Program(this, b);
			}
			return ps;
		}
		
		public boolean isConstant(byte c) {
			return '0' <= c && c <= '9';
		}
		
		public boolean finished() {
			return instruction_pointer == instructions.length;
		}

		public boolean illegal() {
			return illegal;
		}

		// let's just say a program is considered to have ran away
		// if its execution time is at least exponential in the size
		// of the input.
		public boolean runaway(int n) {
			return execution_time > Math.pow(2, n);
		}

		public int outputLength() {
			return output_pointer;
		}

		// last output consistent with xs?
		public boolean incrementallyConsistentWith(byte[] xs) {
			if (output_pointer == 0)
				return true;
			return last_output == xs[output_pointer - 1];
		}
		
		public int compareTo(Program that) {
			return this.cost() - that.cost();
		}

		public int cost() {
			// those about to output get high priority -- if they fail to be consistent
			// with the target sequence, they will be discarded and their memory freed
			if (!finished() && instructions[instruction_pointer] == '.')
				return 0;
			// having output is a good thing -- likely to get more output
			return instructions.length - output_pointer + Util.ilog2(execution_time);
		}
	}

	
	public static void testProgram(String code, int memory_size, byte[] expected_output) {
		Program program = new Program(code.getBytes(), memory_size);
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
		assert(new Program(new Program("12345".getBytes(), 0), (byte)']').illegal());

		testProgram("^^^^^^^^^^[>.^<v]", 2,
		            new byte[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		testProgram("^^^^^^^^^^[>^^^^^^^>^^^^^^^^^^>^^^>^<<<<v]>^^.>^.^^^^^^^..^^^.>^^.<<^^^^^^^^^^^^^^^.>.^^^.vvvvvv.vvvvvvvv.>^.>.", 5,
		            "Hello World!\n".getBytes());
		testProgram("^^>^[.*]", 2,
		            new byte[]{ 1, 2, 4, 8, 16, 32, 64, -128 });
	}
}
