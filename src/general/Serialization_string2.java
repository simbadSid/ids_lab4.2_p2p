package general;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;

import com.oreilly.servlet.Base64Decoder;
import com.oreilly.servlet.Base64Encoder;




public class Serialization_string2
{
	public static String objectToString(Serializable object)
	{
		String encoded = null;

		try
		{
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(object);
			objectOutputStream.close();
			encoded = new String(Base64Encoder.encode(byteArrayOutputStream.toByteArray()));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		return encoded;
	}

	public static Object stringToObject(String string)
	{
		byte[] bytes = Base64Decoder.decodeToBytes(string);
		Object object = null;

		try
		{
			ObjectInputStream objectInputStream = new ObjectInputStream( new ByteArrayInputStream(bytes) );
			object = objectInputStream.readObject();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		return object;
	}

		public static void main(String[] args) throws IOException, ClassNotFoundException
		{
			LinkedList<String> list = new LinkedList<String>();

			list.add("First");
			list.add("second");
			list.add("third");
			list.add("fourth");
			list.add("fifth");

			String serialized = objectToString(list);

			System.out.println("initial      = " + list);
			System.out.println("serialized   = " + serialized);
			LinkedList<String> deserialized = (LinkedList<String>) stringToObject(serialized);
			System.out.println("deserialized = " + deserialized);

		}
}
