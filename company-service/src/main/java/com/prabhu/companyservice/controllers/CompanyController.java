package com.stockMarket.CompanyService.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockMarket.CompanyService.constants.ResponseCode;
import com.stockMarket.CompanyService.constants.WebConstants;
import com.stockMarket.CompanyService.model.Company;
import com.stockMarket.CompanyService.model.Ipo;
import com.stockMarket.CompanyService.model.StockExchange;
import com.stockMarket.CompanyService.repository.CompanyRepository;
import com.stockMarket.CompanyService.repository.IpoRepository;
import com.stockMarket.CompanyService.response.CompanyResponse;
import com.stockMarket.CompanyService.response.IpoResponseWithPaging;
import com.stockMarket.CompanyService.response.StockResponse;
import com.stockMarket.CompanyService.response.serverResponse;
import com.stockMarket.CompanyService.service.StockService;
import com.stockMarket.CompanyService.util.jwtUtil;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/home")
public class CompanyController {
	
	@Autowired
	private jwtUtil jwtutil;

	@Autowired
	private StockService stockService;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private IpoRepository ipoRepository;



	@GetMapping("/getCompanyDetails")
	public ResponseEntity<CompanyResponse> getCompanyDetails(
			@RequestHeader(name = WebConstants.USER_AUTH_TOKEN) String AUTH_TOKEN, @RequestParam("id") String companyId) {

		CompanyResponse resp = new CompanyResponse();
		if ((AUTH_TOKEN == null || AUTH_TOKEN.equalsIgnoreCase("")) && jwtutil.checkToken(AUTH_TOKEN) == null) {
			resp.setStatus(ResponseCode.FAILURE_CODE);
			resp.setMessage(ResponseCode.FAILURE_MESSAGE);
		} else {
			try {
				Company company = companyRepository.findById(Long.parseLong(companyId)).orElse(null);
				List<Ipo> ipos;
				try {
					ipos = ipoRepository.findTop20ByCompanyNameOrderByModifyDateDesc(company.getName());
				} catch (NullPointerException noe) {
					ipos = new ArrayList<>();
				}
				if (company != null) {
					resp.setStatus(ResponseCode.SUCCESS_CODE);
					resp.setMessage("COMPANY_DETAILS");
					resp.setCompany(company);
					resp.setAUTH_TOKEN(AUTH_TOKEN);
					resp.setIpos(ipos);
				}
			} catch (Exception e) {
				resp.setStatus(ResponseCode.FAILURE_CODE);
				resp.setAUTH_TOKEN(AUTH_TOKEN);
				resp.setMessage(e.getMessage());
			}
		}
		return new ResponseEntity<CompanyResponse>(resp, HttpStatus.ACCEPTED);
	}


	@GetMapping("/ipos")
	public ResponseEntity<IpoResponseWithPaging> getAllIposForGivenCompany(
			@RequestHeader(name = WebConstants.USER_AUTH_TOKEN) String AUTH_TOKEN, @RequestParam("id") Long companyId,
			@PageableDefault(size = 20) Pageable pageable) {

		IpoResponseWithPaging resp = new IpoResponseWithPaging();
		if ((AUTH_TOKEN == null || AUTH_TOKEN.equalsIgnoreCase("")) && jwtutil.checkToken(AUTH_TOKEN) == null) {
			resp.setStatus(ResponseCode.FAILURE_CODE);
			resp.setMessage(ResponseCode.FAILURE_MESSAGE);
		} else {
			Company company = companyRepository.findById(companyId).orElse(null);
			try {
				Page<Ipo> ipos = ipoRepository.findAllByCompanyNameOrderByModifyDateDesc(company.getName(), pageable);
				resp.setStatus(ResponseCode.SUCCESS_CODE);
				resp.setMessage("ALL_IPOS");
				resp.setHasNext(ipos.hasNext());
				resp.setHasPrevious(ipos.hasPrevious());
				resp.setIpos(ipos);
			} catch (Exception e) {
				resp.setStatus(ResponseCode.FAILURE_CODE);
				resp.setMessage(e.getMessage());
			}
		}

		return new ResponseEntity<IpoResponseWithPaging>(resp, HttpStatus.ACCEPTED);
	}

	@GetMapping("/getAllCompanies")
	public ResponseEntity<CompanyResponse> getAllComapniesDetails(@RequestParam("name") String stockName) {

		CompanyResponse resp = new CompanyResponse();
			try {
				List<Company> companies = companyRepository.findAllByStockExchanges(stockService.findByName(stockName));
				resp.setStatus(ResponseCode.SUCCESS_CODE);
				resp.setMessage("ALL_COMPANY_DETAILS");
				resp.setCompanies(companies);
			} catch (Exception e) {
				resp.setStatus(ResponseCode.FAILURE_CODE);
				resp.setMessage(e.getMessage());
			}

		return new ResponseEntity<CompanyResponse>(resp, HttpStatus.ACCEPTED);
	}
	
	@GetMapping("/adminHome")
	public ResponseEntity<StockResponse> getStockExchanges(
			@RequestHeader(name = WebConstants.USER_AUTH_TOKEN) String AUTH_TOKEN) throws IOException {

		StockResponse resp = new StockResponse();
		if ((AUTH_TOKEN == null || AUTH_TOKEN.equalsIgnoreCase("")) && jwtutil.checkToken(AUTH_TOKEN) == null) {
			resp.setStatus(ResponseCode.FAILURE_CODE);
			resp.setMessage(ResponseCode.FAILURE_MESSAGE);
		} else {
			try {
				List<StockExchange> stockExchanges = stockService.findAll();

				Map<String, List<Company>> stockCompanyMap = new HashMap<>();

				for (StockExchange stockExchange : stockExchanges) {
					stockCompanyMap.put(stockExchange.getName(),
							companyRepository.findAllByStockExchanges(stockExchange));
				}
				resp.setStatus(ResponseCode.SUCCESS_CODE);
				resp.setMessage("LIST_STOCKS");
				resp.setAUTH_TOKEN(AUTH_TOKEN);
				resp.setStockCompanyMap(stockCompanyMap);

			} catch (Exception e) {
				resp.setStatus(ResponseCode.FAILURE_CODE);
				resp.setMessage(e.getMessage());
				resp.setAUTH_TOKEN(AUTH_TOKEN);
			}
		}
		return new ResponseEntity<StockResponse>(resp, HttpStatus.ACCEPTED);
	}
	
	@PostMapping("/addCompany")
	public ResponseEntity<?> addCompany(@RequestHeader(name = WebConstants.USER_AUTH_TOKEN) String AUTH_TOKEN,
			@RequestHeader(name = "name") String stockName, @Valid @RequestBody Company formCompany) {

		serverResponse resp = new serverResponse();

		if ((AUTH_TOKEN == null || AUTH_TOKEN.equalsIgnoreCase("")) && jwtutil.checkToken(AUTH_TOKEN) == null) {
			resp.setStatus(ResponseCode.FAILURE_CODE);
			resp.setMessage(ResponseCode.FAILURE_MESSAGE);
		} else {
			StockExchange stockExchange = stockService.findByName(stockName);
			List<Company> companies = companyRepository.findAllByStockExchanges(stockExchange);
			for (Company company : companies) {
				if (company.getName().equalsIgnoreCase(formCompany.getName())
						|| company.getCompanyCode().equalsIgnoreCase(formCompany.getCompanyCode())) {
					resp.setStatus("600");
					resp.setMessage("Company already exists already Exists");
					return new ResponseEntity<serverResponse>(resp, HttpStatus.ACCEPTED);
				}
			}
			try {
				Company newCompany = new Company();
				newCompany.setActive(true);
				newCompany.setBoardOfDirector(formCompany.getBoardOfDirector());
				newCompany.setBreif(formCompany.getBreif());
				newCompany.setCeo(formCompany.getCeo());
				newCompany.setCompanyCode(formCompany.getCompanyCode());
				newCompany.setName(formCompany.getName());
				newCompany.setTurnOver(formCompany.getTurnOver());
				newCompany.addStockExchange(stockExchange);
				companyRepository.save(newCompany);
				resp.setAUTH_TOKEN(AUTH_TOKEN);
				resp.setObject(newCompany);
				resp.setStatus(ResponseCode.SUCCESS_CODE);
				resp.setMessage("NEW COMPANY ADDED");
			} catch (Exception e) {
				e.printStackTrace();
				resp.setStatus(ResponseCode.FAILURE_CODE);
				resp.setAUTH_TOKEN(AUTH_TOKEN);
				resp.setMessage(e.getMessage());
			}
		}

		return new ResponseEntity<serverResponse>(resp, HttpStatus.ACCEPTED);
	}

	@GetMapping("/deleteCompany")
	public ResponseEntity<serverResponse> deleteCompany(@RequestParam("id") Long companyId,
			@RequestParam("name") String stockName) {

		serverResponse resp = new serverResponse();
		try {
			Company company = companyRepository.findById(companyId).orElse(null);
			company.removeStockExchange(stockService.findByName(stockName));
			companyRepository.delete(company);
			resp.setStatus(ResponseCode.SUCCESS_CODE);
			resp.setMessage("COMPANY DELETED");
		} catch (Exception e) {
			e.printStackTrace();
			resp.setStatus(ResponseCode.FAILURE_CODE);
			resp.setMessage(e.getMessage());
		}
		return new ResponseEntity<serverResponse>(resp, HttpStatus.OK);
	}

	@PostMapping("/actDeactCompany")
	public ResponseEntity<serverResponse> activateOrDeactivate(@Valid @RequestBody Company formCompany) {
		serverResponse resp = new serverResponse();
		try {
			Company newCompany = companyRepository.findById(formCompany.getId()).orElse(null);
			newCompany.setActive(formCompany.isActive());
			companyRepository.save(newCompany);
			resp.setStatus(ResponseCode.SUCCESS_CODE);
			resp.setMessage("COMPANY UPDATED SUCCESSFULLY");

		} catch (Exception e) {
			resp.setStatus(ResponseCode.FAILURE_CODE);
			resp.setMessage(e.getMessage());
		}
		return new ResponseEntity<serverResponse>(resp, HttpStatus.ACCEPTED);
	}

	@PostMapping("/editCompany")
	public ResponseEntity<serverResponse> saveCompanyDetails(
			@RequestHeader(name = WebConstants.USER_AUTH_TOKEN) String AUTH_TOKEN,
			@RequestHeader(name = "name") String stockName, @Valid @RequestBody Company formCompany) {
		serverResponse resp = new serverResponse();
		try {
			Company newCompany = companyRepository.findById(formCompany.getId()).orElse(null);
			newCompany.setBoardOfDirector(formCompany.getBoardOfDirector());
			newCompany.setBreif(formCompany.getBreif());
			newCompany.setCeo(formCompany.getCeo());
			newCompany.setName(formCompany.getName());
			newCompany.setTurnOver(formCompany.getTurnOver());
			companyRepository.save(newCompany);
			resp.setStatus(ResponseCode.SUCCESS_CODE);
			resp.setMessage("COMPANY EDITED SUCCESSFULLY");

		} catch (Exception e) {
			resp.setStatus(ResponseCode.FAILURE_CODE);
			resp.setMessage(e.getMessage());
		}
		return new ResponseEntity<serverResponse>(resp, HttpStatus.ACCEPTED);
	}

	@PostMapping("/addIpo")
	public ResponseEntity<serverResponse> addNewIpo(
			@RequestHeader(name = WebConstants.USER_AUTH_TOKEN) String AUTH_TOKEN, @Valid @RequestBody Ipo formIpo) {
		serverResponse resp = new serverResponse();
		try {
			Ipo newIpo = new Ipo(formIpo.getCompanyName(), formIpo.getStockExchange(), formIpo.getPrice(),
					formIpo.getTotalShares(), new Date(), new Date(), formIpo.getRemarks());
			ipoRepository.save(newIpo);
			resp.setObject(newIpo);
			resp.setStatus(ResponseCode.SUCCESS_CODE);
			resp.setMessage("IPO ADDED SUCCESSFULLY");

		} catch (Exception e) {
			resp.setStatus(ResponseCode.FAILURE_CODE);
			resp.setMessage(e.getMessage());
		}
		return new ResponseEntity<serverResponse>(resp, HttpStatus.ACCEPTED);
	}

	@PostMapping("/editIpo")
	public ResponseEntity<serverResponse> editipo(@RequestHeader(name = WebConstants.USER_AUTH_TOKEN) String AUTH_TOKEN,
			@RequestHeader(name = "id") String id, @Valid @RequestBody Ipo formIpo) {
		serverResponse resp = new serverResponse();
		try {
			Long ipoId = Long.parseLong(id);
			Ipo ipo = ipoRepository.findById(ipoId).orElse(null);
			if (ipo != null) {
				ipo.setPrice(formIpo.getPrice());
				ipo.setTotalShares(formIpo.getTotalShares());
				ipo.setModifyDate(new Date());
				ipo.setRemarks(formIpo.getRemarks());
				ipoRepository.save(ipo);
				System.out.println("Ipo Edited Successfully..");
				resp.setStatus(ResponseCode.SUCCESS_CODE);
				resp.setMessage("IPO EDITED SUCCESSFULLY");
			}

		} catch (Exception e) {
			resp.setStatus(ResponseCode.FAILURE_CODE);
			resp.setMessage(e.getMessage());
		}
		return new ResponseEntity<serverResponse>(resp, HttpStatus.ACCEPTED);
	}
}
