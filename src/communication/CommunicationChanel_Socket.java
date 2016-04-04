package communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;







public class CommunicationChanel_Socket extends CommunicationChanel
{
// ---------------------------------
// Attributes
// ---------------------------------
	private Socket			socket;
	private PrintWriter		out;
	private BufferedReader	in;
	private boolean			printError	= true;
	private boolean			isClose		= true;

// ---------------------------------
// Builder
// ---------------------------------
	public CommunicationChanel_Socket(String foreignIP, int foreignPort, int localPort, boolean write, boolean read, String writerName, String readerName) throws IOException
	{
		if ((write) && (foreignIP != null))
		{
			this.socket = new Socket(foreignIP, foreignPort);
			this.out	= new PrintWriter(socket.getOutputStream(), true);
		}
		if (read)
		{
			if (this.socket == null)
			{
				ServerSocket serverSocket = new ServerSocket(localPort);
				this.socket = serverSocket.accept();
				serverSocket.close();
			}
			this.in	= new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}
		if ((write) && (foreignIP == null))
		{
			this.out = new PrintWriter(socket.getOutputStream(), true);
		}

		this.isClose = false;
	}

	public CommunicationChanel_Socket(String foreignIP, int foreignPort, int localPort, String writerName, String readerName) throws IOException
	{
		this(foreignIP, foreignPort, localPort, true, true, writerName, readerName);
	}

	public CommunicationChanel_Socket(String foreignIP, int foreignPort, int localPort, boolean printError, String writerName, String readerName) throws IOException
	{
		this(foreignIP, foreignPort, localPort, writerName, readerName);
		this.printError = printError;
	}

// ---------------------------------
// Local method
// ---------------------------------
	@Override
	public String readLine()
	{
		try
		{
			return this.in.readLine();
		}
		catch (Exception e)
		{
			if (printError) e.printStackTrace();
			return null;
		}
	}

	@Override
	public Integer readInt()
	{
		try
		{
			String str = this.in.readLine();
			return Integer.parseInt(str);
		}
		catch (Exception e)
		{
			if (printError) e.printStackTrace();
			return null;
		}
	}

	@Override
	public Boolean readBoolean()
	{
		try
		{
			String str = this.in.readLine();
			return Boolean.parseBoolean(str);
		}
		catch (Exception e)
		{
			if (printError) e.printStackTrace();
			return null;
		}
	}

	@Override
	public Boolean writeLine(String msg)
	{
		try
		{
			this.out.write(msg + "\n");
			this.out.flush();
			return true;
		}
		catch (Exception e)
		{
			if (printError) e.printStackTrace();
			return false;
		}
	}

	@Override
	public void close()
	{
		try
		{
			if (this.in != null)
				this.in.close();
			if (this.out != null)
				this.out.close();
			this.socket.close();
		}
		catch (IOException e)
		{
			if (printError) e.printStackTrace();
		}
		this.isClose = true;
	}

	@Override
	public Boolean isClose()
	{
		return this.isClose;
	}
}
