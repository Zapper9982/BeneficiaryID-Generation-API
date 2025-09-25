/*
* AMRIT - Accessible Medical Records via Integrated Technologies
* Integrated EHR (Electronic Health Records) Solution
*
* Copyright (C) "Piramal Swasthya Management and Research Institute"
*
* This file is part of AMRIT.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see https://www.gnu.org/licenses/.
*/
package com.iemr.common.bengen.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.iemr.common.bengen.domain.M_BeneficiaryRegidMapping;
import com.iemr.common.bengen.repo.BeneficiaryIdRepo;
import com.iemr.common.bengen.utils.Generator;
import com.iemr.common.bengen.utils.config.ConfigProperties;

@Service
public class GenerateBeneficiaryService {
	private static final Logger logger = LoggerFactory.getLogger(GenerateBeneficiaryService.class);
	private ExecutorService executor = Executors.newCachedThreadPool();
	private static final int BATCH_SIZE = 500;
	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	BeneficiaryIdRepo beneficiaryIdRepo;

	public void generateBeneficiaryIDs() throws Exception {
		logger.info("BengenApplication.run start");
		long strt = System.currentTimeMillis();

		// for (int i = 0; i < 10; i++) {
		executor.submit(() -> {
			logger.info("Running: " + Thread.currentThread().getName());
			createFile();
		});

		long fin = System.currentTimeMillis() - strt;
		logger.info("BengenApplication.run finish. time = " + fin + " ms.");
	}

	public void createFile() {
	    logger.info("BengenApplication.createFile start");
	    long strt = System.currentTimeMillis();

	    Integer count = ConfigProperties.getInteger("no-of-benID-to-be-generate");
	    List<Object[]> batchArgs = createBatchData(count);

	    // Batch insert using JdbcTemplate
	    String sql = "INSERT INTO `db_identity`.`m_beneficiaryregidmapping` " +
	                 "(`BeneficiaryID`, `Provisioned`, `Deleted`, `CreatedDate`, `CreatedBy`) " +
	                 "VALUES (?, b'0', b'0', ?, ?)";

	    for (int i = 0; i < batchArgs.size(); i += BATCH_SIZE) {
	        List<Object[]> batch = batchArgs.subList(i, Math.min(i + BATCH_SIZE, batchArgs.size()));
	        jdbcTemplate.batchUpdate(sql, batch);
	    }

	    long fin = System.currentTimeMillis() - strt;
	    logger.info("BengenApplication.createFile finish. time = " + fin + " ms.");
	}

	public List<Object[]> createBatchData(int num) {
	    logger.info("BengenApplication.createBatchData start");
	    long strt = System.currentTimeMillis();

	    Timestamp ts = Timestamp.from(Instant.now());
	    Generator g = new Generator();

	    // Use parallelStream to generate Beneficiary IDs concurrently
	    List<Object[]> data = IntStream.range(0, num).parallel()
	        .mapToObj(i -> new Object[]{
	            g.generateBeneficiaryId(), // Assuming it's thread-safe
	            ts,
	            "admin-batch"
	        })
	        .collect(Collectors.toList());

	    long fin = System.currentTimeMillis() - strt;
	    logger.info("BengenApplication.createBatchData finish. time = " + fin + " ms.");
	    return data;
	}

	public void testLoopGenr() {
		List<String> strList = new ArrayList<String>();
		StringBuffer sb = new StringBuffer(
				"INSERT INTO `db_identity`.`m_beneficiaryregidmapping` " +
						"(`BeneficiaryID`,`Provisioned`,`Deleted`,`CreatedDate`," +
						"`CreatedBy`) VALUES ");
		Timestamp ts = Timestamp.from(Instant.now());
		Generator g = new Generator();
		sb.append("( ")
				.append(g.generateBeneficiaryId())
				.append(",").append("N").append(",")
				.append("0").append(",")
				.append(ts).append(",")
				.append("admin-batch").append(") ");

		strList.add(sb.toString());
	}

	public void testPMDAvoidGenr() {

	}

	public List<M_BeneficiaryRegidMapping> getBeneficiaryIDs(Long num, Integer vanID) {
		logger.info("getBeneficiaryIDs start");
		long strt = System.currentTimeMillis();
		Generator g = new Generator();
		StringBuffer sb = new StringBuffer(
				"INSERT INTO `db_identity`.`m_beneficiaryregidmapping` " +
						"(`BeneficiaryID`,`Provisioned`,`Deleted`,`Reserved`," +
						"`CreatedDate`,`CreatedBy`,`VanID`) VALUES ");
		Timestamp ts = Timestamp.from(Instant.now());
		List<M_BeneficiaryRegidMapping> list = new ArrayList<M_BeneficiaryRegidMapping>();

		for (int i = 0; i < num; i++) {
			sb.append("( ");
			sb.append(g.generateBeneficiaryId()).append(",").append("b'0'")
					.append(",").append("b'0'").append(",")
					.append("b'1'").append(",").append("'")
					.append(ts).append("',").append("'admin-batch'").append(",")
					.append(vanID).append("");
			sb.append(" ), ");

		}

		sb.deleteCharAt(sb.lastIndexOf(","));

		jdbcTemplate.execute(sb.toString());

		List<Object[]> result = null;

		logger.info("ts result1 = " + ts);

		result = beneficiaryIdRepo.getBenIDGenerated(vanID, num);

		for (Object[] objects : result) {
			if (objects != null && objects.length > 0) {

				list.add(new M_BeneficiaryRegidMapping(((Number) objects[0]).longValue(),
						((Number) objects[1]).longValue(),
						(Timestamp) objects[2], "admin-batch"));

			}

		}
		long fin = System.currentTimeMillis() - strt;
		logger.info("getBeneficiaryIDs finish. time = " + fin + " ms.");

		return list;

	}

}
