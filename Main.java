public class Main {
	public static void main(String[] args) {
		Program.test();

		Compressor c = new Compressor();

		Program p;

		p = c.compress(new byte[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		System.out.println(p);

		p = c.compress(new byte[]{ 1, 2, 4, 8, 16, 32, 64, -128 });
		System.out.println(p);

		p = c.compress(new byte[]{ 0, -1, 2, -3, 4, -5, 6, -7, 8, 9 });
		System.out.println(p);

		p = c.compress(new byte[]{ 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 89 });
		System.out.println(p);

		// ambitious indeed
		p = c.compress("Hello World!\n".getBytes());
		System.out.println(p);
	}
}
