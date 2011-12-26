public class Util {
	private static final int[] b = { 0x2, 0xC, 0xF0, 0xFF00, 0xFFFF0000 };
	private static final int[] S = { 1, 2, 4, 8, 16 };

	// NOTE: negative integers are treated as unsigned, and ilog2(0) == 0
	public static int ilog2(int n) {
		int r = 0; // result of log2(v) will go here
		for (int i = b.length - 1; i >= 0; i--) {
			if ((n & b[i]) != 0) {
				n >>= S[i];
				r |= S[i];
			} 
		}
		return r;
	}

	// NOTE: will just roll over if the result doesn't fit
	public static int iexp2(int exponent) {
		return (int)Math.pow(2, exponent);
	}
	
	public static double log2(double x) {
		return Math.log(x) / Math.log(2);
	}

	public static int count(char c, byte[] instructions) {
		int count = 0;
		for (byte b: instructions)
			if (b == c)
				count++;
		return count;
	}
	
	public static boolean contains(byte c, byte[] instructions) {
		for (byte b: instructions)
			if (b == c)
				return true;
		return false;
	}
	
	public static int lastIndexOf(char c, byte[] instructions) {
		for (int i = instructions.length - 1; i >= 0; i--)
			if (instructions[i] == c)
				return i;
		return -1;
	}
	
	public static int lastIndexOfOneOf(byte[] cs, byte[] instructions) {
		for (int i = instructions.length - 1; i >= 0; i--)
			if (Util.contains(instructions[i], cs))
				return i;
		return -1;
	}
}
