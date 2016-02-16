// Copyright © 2016 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

public class SpsReader
{
	// local constants
	private final static String TAG = "SpsReader";

	// instance variables
	byte[] nal;
	int length;
	int currentBit;

	public SpsReader(byte[] nal, int length)
	{
		this.nal = nal;
		this.length = length;
		currentBit = 0;
	}

	public void skipBits(int n)
	{
		currentBit += n;
	}

	public int readBit()
	{
		int index = currentBit / 8;
		int offset = currentBit % 8 + 1;

		currentBit++;
		return (index < length) ? ((nal[index] >> (8 - offset)) & 0x01) : 0;
	}

	public int readBits(int n)
	{
		int bits = 0;
		for (int i = 0; i < n; i++)
		{
			int bit = readBit();
			bits = (bits << 1) + bit;
		}
		return bits;
	}

	private int readCode(boolean signed)
	{
		int zeros = 0;
		while (readBit() == 0)
		{
			zeros++;
		}

		int code = (1 << zeros) - 1 + readBits(zeros);
		if (signed)
		{
			code = (code + 1) / 2 * (code % 2 == 0 ? -1 : 1);
		}

		return code;
	}

	public int readExpGolombCode()
	{
		return readCode(false);
	}

	public int readSignedExpGolombCode()
	{
		return readCode(true);
	}

	public boolean isEnd()
	{
		return (currentBit / 8) >= length;
	}
}
