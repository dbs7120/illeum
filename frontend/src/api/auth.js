import { instance, posts } from './index';

// 로그인
function loginUser(userData) {
  return instance.post(``);
}

//회원가입
function registerUser(userData) {
  return instance.post(`/member/user/signup`, userData);
}
//정보수정
function editUser(userData) {
  return posts.put(``);
}

//회원탈퇴
function signout(email) {
  return posts.delete(``);
}

export { loginUser, registerUser, editUser, signout };
