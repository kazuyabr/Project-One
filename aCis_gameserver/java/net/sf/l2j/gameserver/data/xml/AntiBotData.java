package net.sf.l2j.gameserver.data.xml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.PledgeImage;

/**
 * @author Williams
 *
 */
public class AntiBotData
{
	public final static CLogger LOGGER = new CLogger(AntiBotData.class.getName());
	
	public static Map<Integer, AntiBoot> _imageAntiBotOri = new HashMap<>();
	public static Map<Integer, AntiBoot> _imageAntiBotClient = new HashMap<>();
	
	public final static int[] img_antibot_id =
	{
		7000, 7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008, 7009
	};
	
	public AntiBotData()
	{
		_imageAntiBotOri.clear();
		int cont = 0;
		
		for (int imgId : img_antibot_id)
		{
			File image = new File("data/images/" + imgId + ".dds");
			_imageAntiBotOri.put(cont, new AntiBoot(cont, ConverterImgBytes(image)));
			cont++;
		}
		
		ThreadPool.scheduleAtFixedRate(new startEncriptCaptcha(), 100, 600000); // 10 Minutes
		
		LOGGER.info("Loaded {} images of AntiBot.", _imageAntiBotOri.size());
	}
	
	public void sendImage(Player player, int imgId)
	{
		PledgeImage packet = null;
		
		if ((imgId >= 50000) && (imgId <= 800000))
		{
			for (Entry<Integer, AntiBoot> entrySet : _imageAntiBotClient.entrySet())
			{
				AntiBoot imgCoding = entrySet.getValue();
				
				if (imgId == imgCoding.getCodificacion())
					packet = new PledgeImage(imgId, imgCoding.getImagen());
			}
		}
		
		player.sendPacket(packet);
	}
	
	public class startEncriptCaptcha implements Runnable
	{
		public startEncriptCaptcha()
		{
			
		}
		
		@Override
		public void run()
		{
			_imageAntiBotClient.clear();
			
			for (Entry<Integer, AntiBoot> entrySet : _imageAntiBotOri.entrySet())
			{
				entrySet.getValue().getImagen();
				_imageAntiBotClient.put(entrySet.getKey(), new AntiBoot(Rnd.get(50000, 800000), entrySet.getValue().getImagen()));
			}
		}
	}
	
	public int getAntiBotClientID(int pos)
	{
		int returnCoding = 0;
		
		for (Entry<Integer, AntiBoot> entrySet : _imageAntiBotClient.entrySet())
		{
			int numeroImage = entrySet.getKey().intValue(); 
			
			if (pos == numeroImage)
			{
				AntiBoot imgCoding = entrySet.getValue();
				returnCoding = imgCoding.getCodificacion();
			}
			
			if (pos > 9)
				LOGGER.warn("error in getAntiBotClientID...number dont exist");
		}
		return returnCoding;
	}
	
	public class AntiBoot
	{
		int _codificacion;
		byte[] _data;
		
		public AntiBoot(int codificacion, byte[] data)
		{
			_codificacion = codificacion;
			_data = data;
		}
		
		public int getCodificacion()
		{
			return _codificacion;
		}
		
		public byte[] getImagen()
		{
			return _data;
		}
	}
	
	private static byte[] ConverterImgBytes(File imagen)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[1024];
		try (FileInputStream fis = new FileInputStream(imagen))
		{
			for (int readNum; (readNum = fis.read(buffer)) != -1;)
				bos.write(buffer, 0, readNum);
		}
		catch (IOException e)
		{
			LOGGER.warn("Error when converter image to byte[]");
		}
		
		return bos.toByteArray();
	}
	
	public static AntiBotData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AntiBotData _instance = new AntiBotData();
	}
}