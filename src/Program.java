public abstract class Program implements Comparable<Program> {

	public abstract void step();

	// step until finished or illegal, collecting output
	public abstract byte[] run();

	public abstract int codeLength();

	public abstract int executionTime();
	public abstract int memorySize();

	public abstract int outputLength();

	public abstract boolean finished();

	public abstract boolean illegal();

	public abstract int cost();
	
	public int compareTo(Program that) {
		return this.cost() - that.cost();
	}

	// last output consistent with xs?
	public abstract boolean incrementallyConsistentWith(byte[] xs);

	// returns the set of programs that are one instruction
	// longer than this, but are equal to this in every other
	// respect (i.e. execution state and everything).
	public abstract Program[] successors();

}