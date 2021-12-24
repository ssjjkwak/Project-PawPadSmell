package org.kosta.myproject.controller;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.kosta.myproject.model.domain.BoardDTO;
import org.kosta.myproject.model.domain.PagingBean;
import org.kosta.myproject.model.mapper.BoardMapper;
import org.kosta.myproject.model.mapper.CommentBoardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/board")
public class BoardController {
	@Resource
	private BoardMapper boardMapper;
	@Resource
	private CommentBoardMapper commentBoardMapper;
	@Resource
	private PagingBean pagingBean;

	@RequestMapping("/board/writepro")
	public String boardWrite(BoardDTO boardDTO, Model model, MultipartFile file)
			throws IllegalStateException, IOException {// 작성한 글 및 파일 업로드 처리
		// int memberId = 1;
		// MemberDTO memberDTO = new MemberDTO();
		// memberDTO.setMemberId(memberId);

		// 1. 실제 파일이 저장되는 경로 지정
		// System.getProperty(“user.dir”) -> 현재 작업 디렉토리
		String projectPath = System.getProperty("user.dir") + "\\src\\main\\resources\\static\\files";

		// 2. UUID로 식별자 랜덤으로 이름 만들어줌
		// UUID : 네트워크 상에서 고유성이 보장되는 id를 만들기 위한 표준 규약
		// 랜덤으로 파일 이름 생성
		UUID uuid = UUID.randomUUID();

		// 3. uuid+원래 파일이름 = 새로운 파일이름 // 같은 이름의 파일을 업로드 시 기존의 파일 덮어쓰기 방지를 위함.
		String fileName = uuid + "_" + file.getOriginalFilename();

		// 4. 파일 넣어주는 껍데기 : 파일 생성해주되 경로 설정하고 파일 이름도 받겠다.
		// File(File parent, String Child) parent 객체 폴더의 child 라는 파일에 대한 File 객체를 생성
		// File saveFile = new File(projectPath, fileName);
		// (저장할 폴더 이름, 저장할 파일 이름)
		File saveFile = new File(projectPath, fileName);

		// 5. 업로드된 파일 저장
		file.transferTo(saveFile); // exception 경고 뜸. throws 해준다.

		// 6. db에 파일명, 파일 경로 저장
		boardDTO.setFilename(fileName);
		boardDTO.setFilepath("/files/" + fileName);

		boardMapper.boardWrite(boardDTO);

		return "redirect:/board/";// 게시글 리스트로 리다이렉트
	}

//---------------------------------------------------------------------------------------------------
//게시글 수정
	// @GetMapping("/board/modify/{id}")
	@GetMapping("/board/modify")
	public String boardModify(@PathVariable("postId") int postId, Model model, BoardDTO boardDTO) {

		// model.addAttribute("board", boardMapper.boardWrite(boardDTO)); *************
		return "boardmodify";
	}

	@PostMapping("/board/update/{postId}")
	public String boardUpdate(@PathVariable("postId")int postId, BoardDTO boardDTO) {// 여기 board에 새로 입력한 내용을 받아옴.
		// 기존의 글을 검색한다.
		// 기존의 board에서 받아오도록 Board 객체를 만든다.
		BoardDTO boardTemp = boardMapper.boardUpdate(boardDTO);// 기존에 있던 내용 가져옴.
		boardTemp.setTitle(boardDTO.getTitle());// 위 매개변수 board로 새롭게 받아온 내용을 기존의 내용에 덮어씌운다.
		boardTemp.setContent(boardDTO.getContent());
		return "redirect:/board/{postId}";
	}

	@Autowired
	public BoardController(BoardMapper boardMapper, CommentBoardMapper commentBoardMapper, PagingBean pagingBean) {
		super();
		this.boardMapper = boardMapper;
		this.commentBoardMapper = commentBoardMapper;
		this.pagingBean = pagingBean;
	}

	@GetMapping("/list/{boardId}/{categoryId}")
	public String getAllLists(Model model, @PathVariable("boardId") int boardId,
			@PathVariable("categoryId") int categoryId, String pageNo) {

		int totalPostCount = boardMapper.getCategoryCount(boardId, categoryId);
		PagingBean pagingBean = null;

		if (pageNo == null) {
			// 현재 페이지가 1page로 할당되어 있음
			pagingBean = new PagingBean(totalPostCount);
		} else {
			// client에서 보낸 page번호로 할당한다
			pagingBean = new PagingBean(totalPostCount, Integer.parseInt(pageNo));
		}

		model.addAttribute("pagingBean", pagingBean);
		model.addAttribute("boardname", boardMapper.getBoardName(boardId));
		System.out.println(boardMapper.getBoardName(boardId));
		model.addAttribute("boardId", boardId);
		model.addAttribute("categoryId", categoryId);
		model.addAttribute("categoryname", boardMapper.getCatName(categoryId));
		model.addAttribute("list", boardMapper.getAllLists(boardId, categoryId, pagingBean.getStartRowNumber(),
				pagingBean.getEndRowNumber()));

		return "/board/board-list";
	}

	@RequestMapping(value = "/{postId}")
	public String getPostDetail(Model model, @PathVariable int postId) {
		boardMapper.hitsUpdate(postId);
		model.addAttribute("list", boardMapper.getpostDetail(postId));
		model.addAttribute("comment", commentBoardMapper.findByComment(postId));
		model.addAttribute("commentsCount", commentBoardMapper.getCommentCount(postId));
		return "/board/board-detail";
	}

	@RequestMapping("/delete/{postId}/{boardId}/{categoryId}")
	public String deletePost(Model model, HttpServletRequest request, @PathVariable("postId") int postId,
			@PathVariable("boardId") int boardId, @PathVariable("categoryId") int categoryId) {
		boardMapper.deletePost(postId);
		model.addAttribute("board", boardMapper.getBoardName(boardId));
		model.addAttribute("category", boardMapper.getCatName(categoryId));
		model.addAttribute("list", boardMapper.getAllLists(boardId, categoryId, pagingBean.getStartRowNumber(),
				pagingBean.getEndRowNumber()));

		return "redirect:/board/list/{boardId}/{categoryId}";
	}

}
