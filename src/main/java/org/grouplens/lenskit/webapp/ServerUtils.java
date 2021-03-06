package org.grouplens.lenskit.webapp;

import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

public class ServerUtils {

	public static ParsedUrl parseUrl(HttpServletRequest request, Set<String> knownResourceNames) throws BadRequestException {
		String url;
		try {
			//Keeps multiple "/" characters from being collapsed to a single "/"
			url = request.getRequestURI().substring(request.getContextPath().length()+1);
		} catch (StringIndexOutOfBoundsException e) {
			throw new BadRequestException("Error Parsing URL", e);
		}
		Object2ObjectLinkedOpenHashMap<String, String> resourceMap = new Object2ObjectLinkedOpenHashMap<String, String>();
		String[] urlParts = url.split("/");

		//Check for extension on url specifying response format
		SerializationFormat scheme = SerializationFormat.UNSPECIFIED;
		String end = urlParts[urlParts.length-1];
		if (end.endsWith(".json")) {
			scheme = SerializationFormat.JSON;
			urlParts[urlParts.length-1] = end.substring(0, end.indexOf(".json"));
		} else if (end.endsWith(".xml")) {
			scheme = SerializationFormat.XML;
			urlParts[urlParts.length-1] = end.substring(0, end.indexOf(".xml"));
		}
		//Process each part of the url and add it to the map
		for (int i = 0; i < urlParts.length; i++) {
			if (i == urlParts.length - 1) {
				// Last substring of URL can't be mapped to next substring
				resourceMap.put(urlParts[i], null);
			}
			else if (!knownResourceNames.contains(urlParts[i])) {
				throw new BadRequestException("Unknown Resource: " + urlParts[i]);
			}
			else if (!knownResourceNames.contains(urlParts[i+1])) {
				resourceMap.put(urlParts[i], urlParts[++i]);
			}
			else {
				resourceMap.put(urlParts[i], null);
			}
		}

		//Create a map of the request's query parameters
		Map<String, List<String>> params = parseQueryString(request.getQueryString());
		return new ParsedUrl(resourceMap, params, scheme);
	}

	private static Map<String, Float> parseAcceptHeader(String header) {
		Object2FloatOpenHashMap<String> accepts = new Object2FloatOpenHashMap<String>();
		//Remove spaces to make parsing parameters easier
		header = header.replace(" ", "");
		for (String str : header.split(",")) {
			if (str.contains(";") && str.contains("q=")) {
				String[] params = str.split(";");
				for (int i = 1; i < params.length; i++) {
					if (params[i].contains("q=")) {
						accepts.put(params[0], Float.parseFloat(params[i].substring(params[i].indexOf('=')+1)));
					}
				}
			} else {
				accepts.put(str, 1.0f);
			}
		}
		return accepts;
	}

	public static SerializationFormat determineResponseFormat(ParsedUrl url, String acceptHeader) {
		if (url.getSerializationFormat() != SerializationFormat.UNSPECIFIED){
			return url.getSerializationFormat();
		} else {
			if (acceptHeader == null) {
				return SerializationFormat.UNSPECIFIED; 
			} else {
				Map<String,Float> accepts = parseAcceptHeader(acceptHeader);
				Float json = accepts.get("application/json");
				Float xml = accepts.get("application/xml");
				if (xml == null && json == null) {
					// Neither JSON nor XML is accepted
					return SerializationFormat.OTHER;
				} else if (json == null) {
					// XML is accepted but JSON is not
					return SerializationFormat.XML;
				} else if (xml == null) {
					// JSON is accepted but XML is not
					return SerializationFormat.JSON;
				} else if (json >= xml) {
					// Both JSON and XML are accepted, but JSON is preferred
					return SerializationFormat.JSON;
				} else {
					// Both JSON and XML are accepted, but XML is preferred
					return SerializationFormat.XML;
				}
			}
		}
	}
	
	public static SerializationFormat determineRequestFormat(String contentType) {
		if (contentType == null) {
			return SerializationFormat.UNSPECIFIED;
		} else if (contentType.equals("application/xml")) {
			return SerializationFormat.XML;
		} else if (contentType.equals("application/json")) {
			return SerializationFormat.JSON;
		} else {
			return SerializationFormat.OTHER;
		}
	}
	
	private static Map<String, List<String>> parseQueryString(String query) {
		Object2ObjectOpenHashMap<String, List<String>> paramMap = 
				new Object2ObjectOpenHashMap<String, List<String>>();
		
		if (query == null) {
			return paramMap;
		}
		
		String[] params = query.split("&");
		for (String param : params) {
			if (!param.contains("=")) {
				continue;
			}
			String key = param.substring(0, param.indexOf('='));
			String value = param.substring(param.indexOf('=') + 1, param.length());
			if (!paramMap.containsKey(key)) {
				paramMap.put(key, new ObjectArrayList<String>());
			}
			paramMap.get(key).add(value);
		}
		return paramMap;
	}
	
	public static URL getFileUrl(Class<?> clazz, String fileName) {
		return clazz.getClassLoader().getResource(fileName);
	}
	
	public static String getFilePath(Class<?> clazz, String fileName) {
		return getFileUrl(clazz, fileName).toString().substring("file:".length());
	}
	
	public static class ParsedUrl {

		private Map<String, String> resourceMap;
		private Map<String, List<String>> paramMap;
		private SerializationFormat format;

		private ParsedUrl (Map<String, String> resourceMap, Map<String, List<String>> paramMap, SerializationFormat scheme) {
			this.resourceMap = resourceMap;
			this.paramMap = paramMap;
			this.format = scheme;
		}

		public Map<String, String> getResourceMap() {
			return resourceMap;
		}

		public Map<String, List<String>> getParamMap() {
			return paramMap;
		}

		public SerializationFormat getSerializationFormat() {
			return format;
		}
	}

	public static enum SerializationFormat {
		JSON, XML, UNSPECIFIED, OTHER
	}
}
