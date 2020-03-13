import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class SimpleHttpServer {

  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(5001), 0);
    server.createContext("/get", new GetHandler());
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  static class InfoHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      String response = "Use /get to download a PDF";
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
      
    }
  }

  static class GetHandler implements HttpHandler 
  {
    public void handle(HttpExchange t) throws IOException 
    {
    	
    	StringBuilder postData = new StringBuilder();

        new BufferedReader(new InputStreamReader(t.getRequestBody()))
            .lines()
            .forEach( (String s) -> postData.append(s) );

      String request = postData.toString().substring(postData.toString().indexOf("/get") + 5);
      
      String methodName = request;
      
      if(methodName.contains("?")) {
    	  methodName = request.substring(0, request.indexOf("?"));
      }
      
      String invalidRequestResponse = "This was an invalid request.";
      File file = null;      
      try {
    	  
    	    String base64Img = request.substring(request.indexOf("img=") + 4, request.indexOf("&"));
    	    System.out.println(base64Img);
    	    file = new File("test.jpg");
    	    FileOutputStream fos = new FileOutputStream(file); 
		    // To be short I use a corrupted PDF string, so make sure to use a valid one if you want to preview the PDF file
		    byte[] decoder = Base64.getDecoder().decode(base64Img);
		    fos.write(decoder);
    	  
    	  if(methodName.contains("flip")) {
        	  int flipParam = Integer.parseInt(request.substring(request.indexOf("flipParam=") + "flipParam=".length()));     	  
        	  file = flip(file, flipParam);
        	  
          }
          else if(methodName.contains("grayscale")) {
        	  file = toGrayScale(file);        	  
          }
    	  else if(methodName.contains("thumbnails")) {
    		  file  = ConvertInThumbnails(file);    		    	  
          }
          else if(methodName.contains("resize")) {
        	  int width = Integer.parseInt(request.substring(request.indexOf("width=") + "width=".length(), request.indexOf("&height=")));
        	  int height = Integer.parseInt(request.substring(request.indexOf("height=") + "height=".length()));
        	  
        	  file = resizeImage(file, width, height);    			  
    	  }
    	  else if(methodName.contains("rotate90")) {
    		  int rotateParam = Integer.parseInt(request.substring(request.indexOf("rotate90Param=") + "rotate90Param=".length()));
    		  file = rotate90(file, rotateParam);
    	  }
    	  else if(methodName.contains("rotateAny")) {
    		  int rotateParam = Integer.parseInt(request.substring(request.indexOf("rotateParam=") + "rotateParam=".length()));
    		  file = rotateAny(file, rotateParam);
    	  }
    	  else {
    		  throw new Exception("Invalid request");
    	  }
    	  
      } catch (Exception ex) {
    	  
    	  t.sendResponseHeaders(400, invalidRequestResponse.length());
    	  
    	  OutputStream outputStream = t.getResponseBody();
    	  PrintStream printStream = new PrintStream(outputStream);
    	  printStream.print(invalidRequestResponse);
    	  printStream.close();
    	  return;
      }
      
      
      // add the required response header for a PDF file
      Headers h = t.getResponseHeaders();
      h.add("Content-Type", "application/jpg");
      h.add("Access-Control-Allow-Origin", "*");

      
      byte [] bytearray  = new byte [(int)file.length()];
      FileInputStream fis = new FileInputStream(file);
      BufferedInputStream bis = new BufferedInputStream(fis);
      bis.read(bytearray, 0, bytearray.length);
      
      byte[] encoded = Base64.getEncoder().encode(bytearray);
      String encodedString = new String(encoded,StandardCharsets.US_ASCII);
      System.out.println(encodedString);
      
      // ok, we are ready to send the response.
      t.sendResponseHeaders(200, encodedString.getBytes().length);
      OutputStream os = t.getResponseBody();
      os.write(encodedString.getBytes(),0,encodedString.getBytes().length);
      os.close();
    }
    
    public static File toGrayScale(File input) 
	{
		try 
		{
			ImageInputStream iss = ImageIO.createImageInputStream(input);	
			Iterator<ImageReader> iterator = ImageIO.getImageReaders(iss);
			ImageReader reader = iterator.next();
			String imageFormat = reader.getFormatName();
			BufferedImage image = ImageIO.read(iss);
			
			int width = image.getWidth();
			int height = image.getHeight();
			
			for(int y=0; y < height; y++) 
			{
				for(int x = 0; x < width; x++) 
				{
					Color color = new Color(image.getRGB(x, y));
					int red = (int) (color.getRed()*0.2126);
					int green = (int) (color.getRed()*0.7152);
					int blue = (int) (color.getRed()*0.0722);
					
					int sum = red + blue + green;
					Color shadeOfGray = new Color(sum,sum,sum);
					image.setRGB(x, y, shadeOfGray.getRGB());
				}
			}
			ImageIO.write(image, imageFormat, input);
			
			return input;
			
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		return input;
	}
    
    public static File flip(File input, int direction) 
	{
		try 
		{
			ImageInputStream iss = ImageIO.createImageInputStream(input);	
			Iterator<ImageReader> iterator = ImageIO.getImageReaders(iss);
			ImageReader reader = iterator.next();
			String imageFormat = reader.getFormatName();
			
			BufferedImage image = ImageIO.read(input);
			int height = image.getHeight();
			int width = image.getWidth();
			
			BufferedImage flipped = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
			
			for(int y = 0; y < height; y++) 
			{
				for(int x = 0; x < width; x++) 
				{
					switch(direction) 
					{
						case -1:
							flipped.setRGB((width - 1) - x, y, image.getRGB(x, y));
							break;
						case 1:
							flipped.setRGB(x, (height - 1) - y, image.getRGB(x, y));
							break;
					}
				}
			}
			
			ImageIO.write(flipped, imageFormat,input);
			return input;
		}
		catch(IOException ex) 
		{
			ex.printStackTrace();
		}
		return input;
	}
    
    public static File ConvertInThumbnails(File input)
	{
	    try 
	    {
	    	ImageInputStream iss = ImageIO.createImageInputStream(input);	
			Iterator<ImageReader> iterator = ImageIO.getImageReaders(iss);
			ImageReader reader = iterator.next();
			String imageFormat = reader.getFormatName();
	    	

	        BufferedImage sourceImage = ImageIO.read(input);
	        float width = sourceImage.getWidth();
	        float height = sourceImage.getHeight();

	        if(width > height)
	        {
	            float extraSize = height-100;
	            float percentHight = (extraSize/height)*100;
	            float percentWidth = width - ((width/100)*percentHight);
	            BufferedImage img = new BufferedImage((int)percentWidth, 100, BufferedImage.TYPE_INT_RGB);
	            Image scaledImage = sourceImage.getScaledInstance((int)percentWidth, 100, Image.SCALE_SMOOTH);
	            img.createGraphics().drawImage(scaledImage, 0, 0, null);
	            BufferedImage img2 = new BufferedImage(100, 100 ,BufferedImage.TYPE_INT_RGB);
	            img2 = img.getSubimage((int)((percentWidth-100)/2), 0, 100, 100);
	            ImageIO.write(img2, imageFormat, input);    
	        }
	        else
	        {
	            float extraSize = width-100;
	            float percentWidth = (extraSize/width)*100;
	            float  percentHight = height - ((height/100)*percentWidth);
	            BufferedImage img = new BufferedImage(100, (int)percentHight, BufferedImage.TYPE_INT_RGB);
	            Image scaledImage = sourceImage.getScaledInstance(100,(int)percentHight, Image.SCALE_SMOOTH);
	            img.createGraphics().drawImage(scaledImage, 0, 0, null);
	            BufferedImage img2 = new BufferedImage(100, 100 ,BufferedImage.TYPE_INT_RGB);
	            img2 = img.getSubimage(0, (int)((percentHight-100)/2), 100, 100);

	            ImageIO.write(img2, imageFormat, input);
	        }
	        return input;

	    } 
	    catch (IOException e) 
	    {
	        e.printStackTrace();
	    }
	    return input;
	}
    
    public static File resizeImage(File originalImage, int width, int height) 
	{
		try 
		{
			ImageInputStream iss = ImageIO.createImageInputStream(originalImage);	
			Iterator<ImageReader> iterator = ImageIO.getImageReaders(iss);
			ImageReader reader = iterator.next();
			String imageFormat = reader.getFormatName();
			
			BufferedImage original = ImageIO.read(originalImage);
			BufferedImage resized = new BufferedImage(width,height, original.getType());		
			
			Graphics2D g2 = resized.createGraphics();
			g2.drawImage(original, 0, 0, width, height, null);
			g2.dispose();
			ImageIO.write(resized, imageFormat, originalImage);
			
			return originalImage;
		}
		catch(IOException ex) 
		{
			ex.printStackTrace();
		}
		return originalImage;
	}
    
    
    public static File rotateAny(File input, double angle)
	{
		try 
		{
			ImageInputStream iis = ImageIO.createImageInputStream(input);
			Iterator<ImageReader> iterator = ImageIO.getImageReaders(iis);
			ImageReader reader = iterator.next();
			String format = reader.getFormatName();
			
			BufferedImage image = ImageIO.read(iis);
			
			double rads = Math.toRadians(angle);
            double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
            int w = image.getWidth();
            int h = image.getHeight();
            int newWidth = (int) Math.floor(w * cos + h * sin);
            int newHeight = (int) Math.floor(h * cos + w * sin);

            BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rotated.createGraphics();
            AffineTransform at = new AffineTransform();
            at.translate((newWidth - w) / 2, (newHeight - h) / 2);

            int x = w / 2;
            int y = h / 2;

            at.rotate(rads, x, y);
            g2d.setTransform(at);
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
			
			ImageIO.write(rotated, format, input);
			return input;
		}
		catch(IOException ex) 
		{
			ex.printStackTrace();
		}
		return input;
	}
    
    
    
    public static File rotate90(File input, int direction)
	{
		try 
		{
			ImageInputStream iis = ImageIO.createImageInputStream(input);
			Iterator<ImageReader> iterator = ImageIO.getImageReaders(iis);
			ImageReader reader = iterator.next();
			String format = reader.getFormatName();
			
			BufferedImage image = ImageIO.read(iis);
			
			int width = image.getWidth();
			int height = image.getHeight();
			
			BufferedImage rotated = new BufferedImage(height,width, image.getType());
			
			for(int y = 0; y < height; y++) 
			{
				for(int x = 0; x < width; x++) 
				{
					switch(direction) 
					{
						case 1: 
							rotated.setRGB(y, (width-1) - x, image.getRGB(x, y));
							break;
						case -1: 
							rotated.setRGB((height-1) - y, x, image.getRGB(x, y));
					} 
					
				}
			}
			
			ImageIO.write(rotated, format, input);
			return input;
		}
		catch(IOException ex) 
		{
			ex.printStackTrace();
		}
		return input;
	}
  }
}