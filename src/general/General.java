package general;

import java.util.LinkedList;








public class General
{
	public static boolean validIP (String ip)
	{
		if (ip.equals("localhost")) return true;

		try
		{
			if ( ip == null || ip.isEmpty() )
			{
				return false;
			}
			
			String[] parts = ip.split( "\\." );
			if ( parts.length != 4 )
			{
				return false;
			}

			for ( String s : parts )
			{
				int i = Integer.parseInt( s );
				if ( (i < 0) || (i > 255) )
				{
					return false;
				}
			}
			if ( ip.endsWith(".") )
			{
				return false;
			}
			
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	public static int getIndexOfIntInList(LinkedList<Integer> list, int toFind, boolean exceptionIfNotFound)
	{
		for (int i=0; i<list.size(); i++)
		{
			if (toFind == list.get(i))
				return i;
		}
		if (exceptionIfNotFound) throw new RuntimeException();
		return -1;
	}

}