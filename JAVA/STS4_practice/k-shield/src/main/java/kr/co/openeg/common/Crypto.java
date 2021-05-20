package kr.co.openeg.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Crypto {

	public static String sha256(String input, String salt) {
		String hash = "";
		
		// TODO
		
		return hash;
	}
	
	public static String encrypt(String input, String salt) {
		String hash = "";
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt.getBytes());
			md.update(input.getBytes());
			byte byteData[] = md.digest();

			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < byteData.length; i++) {
				sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
			}
			hash = sb.toString();

		} catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage());
			hash = "";
		}

		return hash;
	}
}
