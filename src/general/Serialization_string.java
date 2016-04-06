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





public class Serialization_string
{
	/**
	 * Read the object from Base64 string.
	 */
	public static Object getObjectFromSerializedString(String serializedString)
	{
		if ((serializedString == null) || (serializedString.equals("null")))
			return null;

		byte[] bytes = Base64Decoder.decodeToBytes(serializedString);
		Object object = null;

		try
		{
			ObjectInputStream objectInputStream = new ObjectInputStream( new ByteArrayInputStream(bytes) );
			object = objectInputStream.readObject();
		}
		catch(Exception e)
		{
//TODO
//			e.printStackTrace();
		}
		return object;
	}

	/**
     * Write the object to a Base64 string.
     */
	public static String getSerializedStringFromObject(Serializable serializableObject )
	{
		if (serializableObject == null)
			return "null";
		String encoded = null;

		try
		{
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(serializableObject);
			objectOutputStream.close();
			encoded = new String(Base64Encoder.encode(byteArrayOutputStream.toByteArray()));
		}
		catch (IOException e)
		{
//TODO
//			e.printStackTrace();
		}
		return encoded;
	}

	public static LinkedList<Object> getObjectTabFromSerializedStringTab(LinkedList<String> serializedStringTab)
	{
		LinkedList<Object>	res = new LinkedList<Object>();

		for (String str:serializedStringTab)
		{
			res.add(getObjectFromSerializedString(str));
		}
		return res;
	}
}