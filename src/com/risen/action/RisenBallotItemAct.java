package com.risen.action;

import static com.jeecms.common.page.SimplePage.cpn;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jeecms.cms.entity.assist.CmsBallot;
import com.jeecms.cms.entity.assist.CmsBallotItem;
import com.jeecms.cms.manager.CmsBallotRecordMng;
import com.jeecms.cms.manager.assist.CmsBallotItemMng;
import com.jeecms.cms.manager.assist.CmsBallotMng;
import com.jeecms.cms.manager.assist.CmsFileMng;
import com.jeecms.common.page.Pagination;
import com.jeecms.common.upload.FileRepository;
import com.jeecms.common.web.CookieUtils;
import com.jeecms.core.entity.CmsSite;
import com.jeecms.core.entity.CmsUser;
import com.jeecms.core.entity.Ftp;
import com.jeecms.core.manager.CmsUserMng;
import com.jeecms.core.manager.DbFileMng;
import com.jeecms.core.web.WebErrors;
import com.jeecms.core.web.util.CmsUtils;

@Controller
public class RisenBallotItemAct {
	private static final Logger log = LoggerFactory.getLogger(RisenBallotItemAct.class);
	
	@RequiresPermissions("ballotItem:v_list")
	@RequestMapping("/ballotItem/v_list.do")
	public String list(Integer pageNo, HttpServletRequest request, ModelMap model){
		String ballotId = request.getParameter("ballotId");
		Pagination pagination = manager.getPage(new Integer(ballotId),cpn(pageNo), CookieUtils.getPageSize(request));
		model.addAttribute("pagination",pagination);
		model.addAttribute("pageNo",pagination.getPageNo());
		model.addAttribute("ballotId", ballotId);
		return "ballotItem/list";
	}
	@RequiresPermissions("ballotItem:v_add")
	@RequestMapping("/ballotItem/v_add.do")
	public String add(ModelMap model,HttpServletRequest request) {
		String ballotId = request.getParameter("ballotId");
		model.addAttribute("ballotId", ballotId);
		return "ballotItem/add";
	}
	@RequiresPermissions("ballotItem:v_edit")
	@RequestMapping("/ballotItem/v_edit.do")
	public String edit(Integer id, Integer pageNo, HttpServletRequest request, ModelMap model) {
		WebErrors errors = validateEdit(id, request);
		if (errors.hasErrors()) {
			return errors.showErrorPage(model);
		}
		String ballotId = request.getParameter("ballotId");
		model.addAttribute("ballotItem", manager.findById(id));
		model.addAttribute("pageNo",pageNo);
		model.addAttribute("ballotId",ballotId);
		return "ballotItem/edit";
	}
	@RequiresPermissions("ballotItem:existItem")
	@RequestMapping("/ballotItem/existItem.do")
	public void existBallotItem(HttpServletRequest request,HttpServletResponse response){
		String ballotId = request.getParameter("ballotId");
		String item = request.getParameter("item");
		boolean result = manager.existBallotItem(item, new Integer(ballotId));
		try{
			if(result){
				response.getWriter().write("exist");
			}else{
				response.getWriter().write("noexist");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	@RequiresPermissions("ballotItem:o_update")
	@RequestMapping("/ballotItem/o_update.do")
	public String update(CmsBallotItem bean, Integer pageNo, HttpServletRequest request,
			ModelMap model) {
		WebErrors errors = validateUpdate(bean.getItemId(), request);
		if (errors.hasErrors()) {
			return errors.showErrorPage(model);
		}
		String ballotId = request.getParameter("ballotId");
		CmsBallot ballot = ballotManager.findById(new Integer(ballotId));
		bean.setBallot(ballot);
		bean = manager.update(bean);
		model.addAttribute("ballotId",ballotId);
		log.info("update BallotItem id={}.", bean.getItemId());
		return "redirect:../ballot/v_list.do";
	}
	@RequiresPermissions("ballotItem:o_save")
	@RequestMapping("/ballotItem/o_save.do")
	public String save(CmsBallotItem bean, Integer pageNo, HttpServletRequest request,
			ModelMap model) {
		String ballotId = request.getParameter("ballotId");
		CmsBallot ballot = ballotManager.findById(new Integer(ballotId));
		bean.setBallot(ballot);
		WebErrors errors = validateSave(bean, request);
		if (errors.hasErrors()) {
			return errors.showErrorPage(model);
		}
		bean.setVoteCount(0);
		bean = manager.save(bean);
		
		log.info("save BallotItem id={}.", bean.getItemId());
		model.addAttribute("ballotId",ballotId);
		return "redirect:../ballot/v_list.do";
	}
	@RequiresPermissions("ballotItem:o_delete")
	@RequestMapping("/ballotItem/o_delete.do")
	public String delete(Integer[] ids, Integer pageNo, HttpServletRequest request,
			ModelMap model) {
		WebErrors errors = validateDelete(ids, request);
		if (errors.hasErrors()) {
			return errors.showErrorPage(model);
		}
		for (Integer id : ids) {
			manager.deleteById(id);
			log.info("delete BallotItem id={}", id);
		}
		return "redirect:../ballot/v_list.do";
	}

	// 上传
	@RequiresPermissions("ballotItem:o_upload_doc")
	@RequestMapping("/ballotItem/o_upload_doc.do")
	public String uploadDoc(
			@RequestParam(value = "docFile", required = false) MultipartFile file,
			String filename, HttpServletRequest request, ModelMap model) {
		CmsSite site = CmsUtils.getSite(request);
		CmsUser user = CmsUtils.getUser(request);
		String origName = file.getOriginalFilename();
		String ext = FilenameUtils.getExtension(origName).toLowerCase(
				Locale.ENGLISH);
		WebErrors errors = validateUpload(file, request);
		if (errors.hasErrors()) {
			model.addAttribute("error", errors.getErrors().get(0));
			return "ballotItem/wenku_iframe";
		}
		// TODO 检查允许上传的后缀
		try {
			String fileUrl;
			if (site.getConfig().getUploadToDb()) {
				String dbFilePath = site.getConfig().getDbFileUri();
				if (!StringUtils.isBlank(filename)
						&& FilenameUtils.getExtension(filename).equals(ext)) {
					filename = filename.substring(dbFilePath.length());
					fileUrl = dbFileMng.storeByFilename(filename,
							file.getInputStream());
				} else {
					fileUrl = dbFileMng.storeByExt(site.getLibraryPath(), ext,
							file.getInputStream());
					// 加上访问地址
					fileUrl = request.getContextPath() + dbFilePath + fileUrl;
				}
			} else if (site.getUploadFtp() != null) {
				Ftp ftp = site.getUploadFtp();
				String ftpUrl = ftp.getUrl();
				if (!StringUtils.isBlank(filename)
						&& FilenameUtils.getExtension(filename).equals(ext)) {
					filename = filename.substring(ftpUrl.length());
					fileUrl = ftp.storeByFilename(filename,
							file.getInputStream());
				} else {
					fileUrl = ftp.storeByExt(site.getLibraryPath(), ext,
							file.getInputStream());
					// 加上url前缀
					fileUrl = ftpUrl + fileUrl;
				}
			} else {
				String ctx = request.getContextPath();
				if (!StringUtils.isBlank(filename)
						&& FilenameUtils.getExtension(filename).equals(ext)) {
					filename = filename.substring(ctx.length());
					fileUrl = fileRepository.storeByFilename(filename, file);
				} else {
					fileUrl = fileRepository.storeByExt("/u/ballotItem/"+site.getPath(),
							ext, file);
				}
				// 加上部署路径
				fileUrl = ctx + fileUrl;
			}
			cmsUserMng.updateUploadSize(user.getId(),
					Integer.parseInt(String.valueOf(file.getSize() / 1024)));
			if (fileMng.findByPath(fileUrl) != null) {
				fileMng.saveFileByPath(fileUrl, origName, false);
			}
			model.addAttribute("docPath", fileUrl);
			model.addAttribute("docExt", ext);
		} catch (IllegalStateException e) {
			model.addAttribute("error", e.getMessage());
			log.error("upload file error!", e);
		} catch (IOException e) {
			model.addAttribute("error", e.getMessage());
			log.error("upload file error!", e);
		}
		return "ballotItem/wenku_iframe";
	}

	private WebErrors validateSave(CmsBallotItem bean,
			HttpServletRequest request) {
		WebErrors errors = WebErrors.create(request);
		CmsSite site = CmsUtils.getSite(request);
		// bean.setSite(site);
		return errors;
	}

	private WebErrors validateEdit(Integer id, HttpServletRequest request) {
		WebErrors errors = WebErrors.create(request);
		CmsSite site = CmsUtils.getSite(request);
		if (vldExist(id, site.getId(), errors)) {
			return errors;
		}
		return errors;
	}

	private WebErrors validateUpdate(Integer id, HttpServletRequest request) {
		WebErrors errors = WebErrors.create(request);
		CmsSite site = CmsUtils.getSite(request);
		if (vldExist(id, site.getId(), errors)) {
			return errors;
		}
		return errors;
	}

	private WebErrors validateDelete(Integer[] ids, HttpServletRequest request) {
		WebErrors errors = WebErrors.create(request);
		CmsSite site = CmsUtils.getSite(request);
		if (errors.ifEmpty(ids, "ids")) {
			return errors;
		}
		for (Integer id : ids) {
			vldExist(id, site.getId(), errors);
		}
		return errors;
	}

	private boolean vldExist(Integer id, Integer siteId, WebErrors errors) {
		 
		return false;
	}
	private WebErrors validateUpload(MultipartFile file,
			HttpServletRequest request) {
		String origName = file.getOriginalFilename();
		CmsUser user= CmsUtils.getUser(request);
		String ext = FilenameUtils.getExtension(origName).toLowerCase(Locale.ENGLISH);
		int fileSize = (int) (file.getSize() / 1024);
		WebErrors errors = WebErrors.create(request);
		if (errors.ifNull(file, "file")) {
			return errors;
		}
		if(origName!=null&&(origName.contains("/")||origName.contains("\\")||origName.indexOf("\0")!=-1)){
			errors.addErrorCode("upload.error.filename", origName);
		}
		//非允许的后缀
		if(!user.isAllowSuffix(ext)){
			errors.addErrorCode("upload.error.invalidsuffix", ext);
			return errors;
		}
		//超过附件大小限制
		if(!user.isAllowMaxFile((int)(file.getSize()/1024))){
			errors.addErrorCode("upload.error.toolarge",origName,user.getGroup().getAllowMaxFile());
			return errors;
		}
		//超过每日上传限制
		if (!user.isAllowPerDay(fileSize)) {
			long laveSize=user.getGroup().getAllowPerDay()-user.getUploadSize();
			if(laveSize<0){
				laveSize=0;
			}
			errors.addErrorCode("upload.error.dailylimit", laveSize);
		}
		return errors;
	}
	@Autowired
	private CmsBallotItemMng manager;
	@Autowired
	private CmsBallotMng ballotManager;
	@Autowired
	private DbFileMng dbFileMng;
	@Autowired
	private FileRepository fileRepository;
	@Autowired
	private CmsUserMng cmsUserMng;
	@Autowired
	private CmsFileMng fileMng;
	@Autowired
	private CmsBallotRecordMng recordManager;
}
