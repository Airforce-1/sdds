package com.jeecms.cms.manager.assist.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jeecms.cms.dao.assist.CmsBallotDao;
import com.jeecms.cms.entity.assist.CmsBallot;
import com.jeecms.cms.manager.assist.CmsBallotMng;
import com.jeecms.common.page.Pagination;

@Service
@Transactional
public class CmsBallotMngService implements CmsBallotMng {

	@Override
	public Pagination getPage(Integer departId, int pageNo, int pageSize) {
		// TODO Auto-generated method stub
		return dao.getPage(departId, pageNo, pageSize);
	}
	
	@Override
	public Pagination getPages(Integer departId, int pageNo, int pageSize) {
		// TODO Auto-generated method stub
		return dao.getPages(departId, pageNo, pageSize);
	}

	@Override
	public CmsBallot findById(Integer id) {
		// TODO Auto-generated method stub
		return dao.findById(id);
	}

	@Override
	public CmsBallot save(CmsBallot bean) {
		// TODO Auto-generated method stub
		return dao.save(bean);
	}

	@Override
	public CmsBallot update(CmsBallot bean) {
		// TODO Auto-generated method stub
		return dao.update(bean);
	}

	@Override
	public CmsBallot deleteById(Integer id) {
		// TODO Auto-generated method stub
		return dao.deleteById(id);
	}
	
	@Override
	public boolean existBallot(String ballotName, Integer departId) {
		// TODO Auto-generated method stub
		return dao.existBallot(ballotName, departId);
	}
	@Autowired
	private CmsBallotDao dao;
	
}
