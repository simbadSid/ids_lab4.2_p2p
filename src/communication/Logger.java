package communication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Calendar;






public class Logger
{
// ---------------------------------
// Attributs
// ---------------------------------
	public static final String loggerDirectoryPath = "resource/output/";

	private PrintStream		output;
	private static File		sharedDirectory = null;

// ---------------------------------
// Builder
// ---------------------------------
	public Logger(String id)
	{
		if (sharedDirectory == null)
		{
			String dirPath = loggerDirectoryPath + "/" + Calendar.getInstance().getTime();
			sharedDirectory = new File(dirPath);
			sharedDirectory.mkdir();
		}

		try
		{
			this.output = new PrintStream(new File(sharedDirectory.getAbsolutePath() + "/" + id));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

// ---------------------------------
// Local methods
// ---------------------------------
	public void write(String str)
	{
		output.print(str);
	}
}
