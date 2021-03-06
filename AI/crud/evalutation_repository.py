from typing import Optional, Tuple

from cachetools import cached
from utils.CustomTTLCache import CustomTTLCache

from database.db import db


@cached(cache=CustomTTLCache(maxsize=1024, ttl=3600))
def select_member_name_by_uid(uid: int) -> Optional[str]:
    with db.query("SELECT name FROM member WHERE uid = %s", (uid,)) as cursor:
        data = cursor.fetchone()
        if data:
            return data[0]
    return None


@cached(cache=CustomTTLCache(maxsize=1024, ttl=3600))
def select_room_name_founder_uid_by_rid(rid: int) -> Optional[Tuple[str, int]]:
    with db.query("SELECT room_name, uid FROM room WHERE rid = %s", (rid,)) as cursor:
        data = cursor.fetchone()
        if data:
            return data
    return None


@cached(cache=CustomTTLCache(maxsize=1024, ttl=600))
def select_entrant_eid_by_uid_and_rid(uid: int, rid: int) -> Optional[int]:
    with db.query("SELECT eid FROM entrant WHERE uid = %s AND rid = %s", (uid, rid,)) as cursor:
        data = cursor.fetchone()
        if data:
            return data[0]
    return None


@cached(cache=CustomTTLCache(maxsize=1024, ttl=200))
def select_evaluation_vid_recent_by_eid(eid: int) -> Optional[int]:
    with db.query("SELECT vid FROM evaluation WHERE eid = %s ORDER BY eval_date DESC LIMIT 1", (eid,)) as cursor:
        data = cursor.fetchone()
        if data:
            return data[0]
    return None


def update_evaluation_increase_column_by_eid(column: str, vid: int) -> None:
    db.query(f"UPDATE evaluation SET {column} = {column} + 1 WHERE vid = %s", (vid,))


def update_evaluation_increase_attention_by_eid(eid: int) -> None:
    vid: Optional[int] = select_evaluation_vid_recent_by_eid(eid)
    if vid:
        update_evaluation_increase_column_by_eid("attention", vid)


def update_evaluation_increase_distracted_by_eid(eid: int) -> None:
    vid: Optional[int] = select_evaluation_vid_recent_by_eid(eid)
    if vid:
        update_evaluation_increase_column_by_eid("distracted", vid)


def update_evaluation_increase_asleep_by_eid(eid: int) -> None:
    vid: Optional[int] = select_evaluation_vid_recent_by_eid(eid)
    if vid:
        update_evaluation_increase_column_by_eid("asleep", vid)


def update_evaluation_increase_afk_by_eid(eid: int) -> None:
    vid: Optional[int] = select_evaluation_vid_recent_by_eid(eid)
    if vid:
        update_evaluation_increase_column_by_eid("afk", vid)


if __name__ == "__main__":
    rid = 2
    uid = 2
    # 2??? ?????? 2??? ????????? ????????? ?????? ????????? ?????? (????????? ?????? ?????? None ??????)
    id = select_entrant_eid_by_uid_and_rid(rid, uid)
    id = select_entrant_eid_by_uid_and_rid(rid, uid)
    id = select_entrant_eid_by_uid_and_rid(rid, uid)
    print(f"{rid}??? ?????? {uid}??? ????????? ?????? ?????? : {id}")
    if id is None:  # ????????? ????????? ???????????? ?????? ????????? ??????
        pass
    else:
        # ?????? ???????????? ????????? ?????? ?????? ?????? ????????? ??????
        id = select_evaluation_vid_recent_by_eid(id)
        print(f"?????? ???????????? ?????? ?????? ?????? : {id}")
        # ?????? ???????????? attention ???????????? 1 ??????
        update_evaluation_increase_attention_by_eid(id)
        print(f"?????? {id}????????? attention ???????????? 1 ??????")
