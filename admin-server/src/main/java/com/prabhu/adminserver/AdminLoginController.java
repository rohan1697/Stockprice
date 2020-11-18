package com.stockMarket.LoginService.controller;

import java.util.HashMap;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockMarket.LoginService.constants.ResponseCode;
import com.stockMarket.LoginService.constants.WebConstants;
import com.stockMarket.LoginService.model.User;
import com.stockMarket.LoginService.response.serverResponse;
import com.stockMarket.LoginService.service.UserService;
import com.stockMarket.LoginService.utils.jwtUtil;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin")
public class AdminLoginController {

	@Autowired
	private UserService userService;
	
	@Autowired
	private jwtUtil jwtutil;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@PostMapping("/verify")
	public ResponseEntity<serverResponse> verifyUser(@Valid @RequestBody HashMap<String, String> credential) {
		String email = "";
		String password = "";
		if (credential.containsKey(WebConstants.USER_EMAIL)) {
			email = credential.get(WebConstants.USER_EMAIL);
		}
		if (credential.containsKey(WebConstants.USER_PASSWORD)) {
			password = credential.get(WebConstants.USER_PASSWORD);
		}
		User loggedUser = userService.findByEmailAndUsertype(email, WebConstants.USER_ADMIN_ROLE);
		serverResponse resp = new serverResponse();
		if (loggedUser != null && passwordEncoder.matches(password, loggedUser.getPassword())) {
			String jwtToken = jwtutil.createToken(email, password, WebConstants.USER_ADMIN_ROLE);
			resp.setStatus(ResponseCode.SUCCESS_CODE);
			resp.setMessage(ResponseCode.SUCCESS_MESSAGE);
			resp.setAUTH_TOKEN(jwtToken);
		} else {
			resp.setStatus(ResponseCode.FAILURE_CODE);
			resp.setMessage(ResponseCode.FAILURE_MESSAGE);
		}
		return new ResponseEntity<serverResponse>(resp, HttpStatus.OK);
	}
}
