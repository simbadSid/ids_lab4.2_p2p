package communication;

import java.lang.reflect.Constructor;









public abstract class CommunicationChanel
{
	public static final String	COMMUNICATION_CHANEL_SOCKET		= "communication.CommunicationChanel_Socket";
	public static final String	COMMUNICATION_CHANEL_RABBITMQ	= "communication.CommunicationChanel_RabbitMQ";

	public String	readLine	()				{return null;}
	public Integer	readInt		()				{return null;}
	public Boolean	readBoolean	()				{return null;}
	public Boolean	writeLine	(String msg)	{return null;}
	public void		close		()				{}
	public Boolean	isClose		()				{return null;}

	public static CommunicationChanel instantiate(String type, String foreignIP, int foreignPort, int localPort, boolean write, boolean read, String writerName, String readerName)
	{
		try
		{
			Class<?>		cl			= Class.forName(type);
			Constructor<?>	constructor	= cl.getConstructor(String.class, int.class, int.class, boolean.class, boolean.class, String.class, String.class);
			return (CommunicationChanel) constructor.newInstance(foreignIP, foreignPort, localPort, write, read, writerName, readerName);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
}
