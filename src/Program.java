import java.util.ArrayList;
import java.util.List;

public abstract class Program implements Comparable<Program> {
	// step until finished or illegal, collecting output
	public byte[] run() {
		List<Byte> bs = new ArrayList<Byte>();
		while (!finished() && !illegal()) {
			step();
			if (bs.size() < outputLength())
				bs.add(lastOutput());
		}
		byte[] sigh = new byte[bs.size()];
		int i = 0;
		for (Byte rahhhh: bs)
			sigh[i++] = rahhhh;
		return sigh;
	}

	public abstract int codeLength();
	public abstract int executionTime();
	public abstract int memorySize();
	public abstract int outputLength();

	public abstract boolean finished();
	public abstract boolean illegal();

	public abstract void step();
	public abstract byte lastOutput();
	
	public abstract int cost();
	
	public int compareTo(Program that) {
		return this.cost() - that.cost();
	}

	// last output consistent with xs?
	public boolean incrementallyConsistentWith(byte[] xs) {
		int n = outputLength();
		if (n == 0)
			return true;
		return lastOutput() == xs[n - 1];
	}	

	// returns the set of programs that are one instruction
	// longer than this, but are equal to this in every other
	// respect (i.e. execution state and everything).
	public abstract Program[] successors();

}