import java.util.*;

public class Compressor {
	private static final int MAX_ACTIVE_PROGRAM_COUNT = 200000,
	                         BRANCHING_FACTOR = BrainfuckProgram.INSTRUCTIONS.length;

	// find (if exists) a program of length at most as many bytes as
	// the input, that has xs as a prefix of its output, and that
	// outputs this prefix within 2^xs.length execution steps.
	public Program compress(byte[] xs) {
		return compress(xs, 0, xs.length);
	}

	public Program compress(byte[] xs, int a, int b) {
		PriorityQueue<Program> to_be_extrapolated = new PriorityQueue<Program>(MAX_ACTIVE_PROGRAM_COUNT*BRANCHING_FACTOR);
		PriorityQueue<Program> ps = new PriorityQueue<Program>(MAX_ACTIVE_PROGRAM_COUNT);

		try {
			// start with the empty program
			// (I don't think a better-than-nothing decompressor
			// should need more memory than the length of the
			// sequence it represents, but I haven't proven it)
			ps.add(new BrainfuckProgram(b-a));

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
					} else if (!p.illegal() && !runaway(p, xs.length)) {
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
				while (ps.size() < MAX_ACTIVE_PROGRAM_COUNT - BRANCHING_FACTOR
						&& (r = to_be_extrapolated.poll()) != null) {
					for (Program q: r.successors()) {
						if (q == null || q.illegal()) continue;

						if (q.codeLength() <= xs.length)
							ps.add(q);
					}
				}
			}
		} catch (OutOfMemoryError e) {
			System.err.println(ps.size()+" programs active");
			System.err.println(to_be_extrapolated.size()+" programs to be extrapolated");
			Program p; int k = 0;
			while ((p = ps.poll()) != null && k++ < 50) {
				System.err.println("t:"+p.executionTime()+"\t$:"+p.cost()+"\t"+p);
			}
			ps = null;
			e.printStackTrace();
		}
		
		return null;
	}
	
	public boolean runaway(Program p, int n) {
		return p.executionTime() > Util.iexp2(n) || p.memorySize() > n;
	}
}
