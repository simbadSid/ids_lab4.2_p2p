package communication;

import general.SynchronizedList;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;





public class CommunicationChanel_RabbitMQ extends CommunicationChanel
{
// ---------------------------------
// Attributes
// ---------------------------------
	private Connection				connection;
	private Channel					writerChannel;
	private Channel					readerChannel;
	private String					writerName;
	private SynchronizedList<String>synchronizedReceivedMsg;
	private boolean					printError	= true;

// ---------------------------------
// Builder
// ---------------------------------
	public CommunicationChanel_RabbitMQ(String foreignIP, int foreignPort, int localPort, boolean write, boolean read, String writerName, String readerName) throws IOException, TimeoutException
	{
		ConnectionFactory factory = new ConnectionFactory();
		if (foreignIP != null)
			factory.setHost(foreignIP);
		else
			factory.setHost("localhost");
		this.connection = factory.newConnection();

		if (write)
		{
			this.writerName				= new String(writerName);
			this.writerChannel			= connection.createChannel();
			this.writerChannel.queueDeclare(writerName, false, false, false, null);
		}
		if (read)
		{
			this.readerChannel			= connection.createChannel();
			this.readerChannel.queueDeclare(readerName, false, false, false, null);
			this.synchronizedReceivedMsg= new SynchronizedList<String>();
			Consumer consumer = new DefaultConsumer(this.readerChannel)
			{
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException
				{
					String message = new String(body, "UTF-8");
					synchronizedReceivedMsg.addLast(message);
		        }
		    };
		    this.readerChannel.basicConsume(readerName, true, consumer);
		}
	}

// ---------------------------------
// Local method
// ---------------------------------
	@Override
	public String readLine()
	{
		if (this.readerChannel == null)
			throw new RuntimeException("Channel has not been initialized for reading");

		return this.synchronizedReceivedMsg.getAndRemoveFirst();
	}

	@Override
	public Integer readInt()
	{
		String str = this.readLine();
		try
		{
			return Integer.parseInt(str);
		}
		catch(Exception e)
		{
			if (printError) e.printStackTrace();
			return null;
		}
	}

	@Override
	public Boolean readBoolean()
	{
		String str = this.readLine();
		try
		{
			return Boolean.parseBoolean(str);
		}
		catch(Exception e)
		{
			if (printError) e.printStackTrace();
			return null;
		}
	}

	@Override
	public Boolean writeLine(String msg)
	{
		if (this.writerChannel == null)
			throw new RuntimeException("Channel has not been initialized for reading");

		try
		{
			if (msg == null) msg = "null";
		    this.writerChannel.basicPublish("", this.writerName, null, msg.getBytes());
		    return true;
		}
		catch(Exception e)
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
			if (this.writerChannel != null)	this.writerChannel.close();
			if (this.readerChannel != null)	this.readerChannel.close();
			this.connection.close();
		}
		catch(Exception e)
		{
			if (printError) e.printStackTrace();
		}
	}

	@Override
	public Boolean isClose()
	{
		return !this.connection.isOpen();
	}
}