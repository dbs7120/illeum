package com.ssafy.pjt.controller;

import com.ssafy.pjt.dto.Member;
import com.ssafy.pjt.dto.Room;
import com.ssafy.pjt.dto.request.insertRoomDto;
import com.ssafy.pjt.dto.request.updateRoomDto;
import com.ssafy.pjt.dto.response.findRoom;
import com.ssafy.pjt.jwt.JwtTokenUtil;
import com.ssafy.pjt.repository.MemberRepository;
import com.ssafy.pjt.repository.RoomRepository;
import com.ssafy.pjt.repository.mapper.RoomMapper;
import com.ssafy.pjt.service.RoomService;

import io.swagger.annotations.ApiOperation;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

@RestController
@CrossOrigin
@RequestMapping("/api/room")
public class RoomController {
	@Autowired // This means to get the bean called userRepository
	// Which is auto-generated by Spring, we will use it to handle the data
	private RoomRepository roomRepository;

	@Autowired
	private RoomMapper roomMapper;
	
	@Autowired
	private RoomService roomService;
	
	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AmqpAdmin admin;
    
    private static final String SUCCESS = "success";
	private static final String FAIL = "fail";
    
	@ApiOperation(value = "방  리스트 조회")
	@GetMapping(path = "/findAll")
	public ResponseEntity<Object> findAll() {
		try {
			return new ResponseEntity<>(roomMapper.roomAll(), HttpStatus.OK);
		} catch (SQLException e) {
			return new ResponseEntity<>(FAIL, HttpStatus.BAD_REQUEST);
		}
	}

	@ApiOperation(value = "개설자 uid로 방 조회")
	@GetMapping(path = "/findByUid")
	public ResponseEntity<Object> findByUid(@RequestParam int uid, @RequestParam String accessToken) {
		try {
			String email = jwtTokenUtil.getUsernameFromToken(accessToken);
			Member member = memberRepository.findByEmail(email);
			if (uid == member.getUid()) {
				return new ResponseEntity<>(roomRepository.findByUid(uid), HttpStatus.OK);
			} else {
				return new ResponseEntity<>("토큰 uid랑 uid가 다릅니다", HttpStatus.NO_CONTENT);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(FAIL, HttpStatus.BAD_REQUEST);
		}
	}

	@ApiOperation(value = "rid로 방 조회")
	@GetMapping(path = "/findByRid")
	public ResponseEntity<Object> findByRid(@RequestParam int rid) {
		Room room = roomRepository.findByRid(rid);
		try {
			findRoom findroom = roomService.conversion(room);
			return new ResponseEntity<>(findroom, HttpStatus.OK);
		} catch (SQLException e) {
			return new ResponseEntity<>(FAIL, HttpStatus.BAD_REQUEST);
		}
		
	}

	@ApiOperation(value = "room_name으로 방 조회")
	@GetMapping(path = "/findByRoomName")
	public ResponseEntity<Object> findByRoomName(@RequestParam String roomName) {
		try {
			return new ResponseEntity<>(roomMapper.roomName(roomName), HttpStatus.OK);
		} catch (SQLException e) {
			return new ResponseEntity<>(FAIL, HttpStatus.BAD_REQUEST);
		}
	}

	@ApiOperation(value = "방에 참여한 맴버 목록 조회")
	@GetMapping(path = "/member")
	public ResponseEntity<Object> roomjoinMemeber(@RequestParam int rid) {
		List<Map<String, Object>> list = null;
		try {
			list = roomMapper.roomjoinMemeber(rid);
			if (list.isEmpty())
				return new ResponseEntity<>("참가자가 없습니다.", HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(FAIL, HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(list, HttpStatus.OK);
	}

	@ApiOperation(value = "방에 참여한 맴버의 평가 목록 조회")
	@GetMapping(path = "/evaluation")
	public ResponseEntity<Object> roomJoinEvaluation(@RequestParam int rid) {
		List<Map<String, Object>> list = null;
		try {
			list = roomMapper.roomJoinEvaluation(rid);
			if (list.isEmpty())
				return new ResponseEntity<>("평가가 없습니다.", HttpStatus.NO_CONTENT);
		} catch (Exception e) {			
			return new ResponseEntity<>(FAIL, HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(list, HttpStatus.OK);
	}

	@ApiOperation(value = "방 생성")
	@Transactional
	@PostMapping(path = "/insert")
	public ResponseEntity<Object> insertByUid(@RequestBody insertRoomDto insertRoom) {
		Room room = new Room();
		
		if (insertRoom.getRoom_type().equals("비공개")) {
			room.setRoomPassword(insertRoom.getRoom_password());
			room.setRoomType("비공개");
		} else {
			room.setRoomPassword(null);
			room.setRoomType("공개");
		}
		room.setUid(insertRoom.getUid());
		room.setRoomName(insertRoom.getRoom_name());
		room.setStartTime(insertRoom.getStart_time());
		room.setEndTime(insertRoom.getEnd_time());
		room.setRoomState(insertRoom.getRoom_state());

		try {
			room = roomRepository.save(room);
			findRoom find = roomService.conversion(room);
			String roomName = "room." + Integer.toString(room.getRid());
			FanoutExchange fanout = new FanoutExchange(roomName);
			admin.declareExchange(fanout);
			return new ResponseEntity<>(find, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(FAIL, HttpStatus.BAD_REQUEST);
		}
	}

	@ApiOperation(value = "방 삭제")
	@Transactional
	@DeleteMapping(path = "/deleteByRid")
	public ResponseEntity<Object> deleteByUid(@RequestParam int rid) {
		Room room = roomRepository.findByRid(rid);
        String roomName = "room." + Integer.toString(room.getRid());

		try {
			admin.deleteExchange(roomName);
			roomRepository.deleteByRid(rid);
		} catch (Exception e) {
			return new ResponseEntity<>(FAIL, HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(SUCCESS, HttpStatus.OK);
	}

	@ApiOperation(value = "방 수정(rid만 필수)")
	@Transactional
	@PutMapping(path = "/updateByRid")
	public ResponseEntity<Object> updateTheRoom(@RequestBody updateRoomDto roomdto) {
		Room room = roomRepository.findByRid(roomdto.getRid());

		if (room == null)
			return new ResponseEntity<>("room이 없습니다", HttpStatus.OK);

		if (roomdto.getRoom_name() != null)
			room.setRoomName(roomdto.getRoom_name());
		if (roomdto.getRoom_password() != null)
			room.setRoomPassword(roomdto.getRoom_password());
		if (roomdto.getRoom_state() != null)
			room.setRoomState(roomdto.getRoom_state());
		if (roomdto.getRoom_type() != null)
			room.setRoomType(roomdto.getRoom_type());
		if (roomdto.getStart_time() != null)
			room.setStartTime(roomdto.getStart_time());
		if (roomdto.getEnd_time() != null)
			room.setEndTime(roomdto.getEnd_time());

		try {
			roomRepository.save(room);
			return new ResponseEntity<>(SUCCESS, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(FAIL, HttpStatus.BAD_GATEWAY);
		}
		
	}
}