public class Main {
	public static void main(String[] args) {
		Compressor.test();
		
		Compressor c = new Compressor();

		Compressor.Program p;

		p = c.compress(new byte[]{ 0, 1, 2, 3, 4, 5 });
		System.out.println(p);

		p = c.compress(new byte[]{ 1, 2, 4, 8, 16, 32, 64, -17 });
		System.out.println(p);
	}
}
