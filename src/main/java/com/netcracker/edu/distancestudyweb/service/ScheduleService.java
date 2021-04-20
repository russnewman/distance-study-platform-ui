package com.netcracker.edu.distancestudyweb.service;


import com.netcracker.edu.distancestudyweb.dto.*;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface ScheduleService {
    Map<AbstractMap.SimpleEntry<String, Boolean>, List<ScheduleVDto>> getStudentFullSchedule(Long studentId);

    List<ScheduleDto> getTeacherSchedule(Long teacherId, Optional<Boolean> weekIsOdd);
    List<ScheduleDto> getTomorrowTeacherSchedule(Long teacherId, Optional<Boolean> weekIsOdd);

    List<ScheduleDto> getSubjectTeacherSchedule(List<ScheduleDto> list, Long subjectId);
    Map<ScheduleDto, List<GroupDto>> mapScheduleToGroups(List<ScheduleDto> list);
    List<List<ScheduleDto>> schedulesByDays(Map<ScheduleDto, List<GroupDto>> map);

    List<ScheduleVDto> getStudentTodaySchedule(Long studentId);
    List<ScheduleVDto> getStudentTomorrowSchedule(Long studentId);
    SubjectDto getStudentCurrentSubject(Long studentId);
    SubjectDto getStudentNextSubject(Long studentId);
    Map<AbstractMap.SimpleEntry<String, Boolean>, List<ScheduleVDto>> getStudentSubjectSchedule(Long studentId, Long subjectId);
    String getTodayDayName();
    void updateLessonLink(Long scheduleId, String link);
}
