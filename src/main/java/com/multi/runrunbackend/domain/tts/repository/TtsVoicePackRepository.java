package com.multi.runrunbackend.domain.tts.repository;

import com.multi.runrunbackend.domain.tts.entity.TtsVoicePack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : TtsVoicePackRepository
 * @since : 2025. 12. 24. Wednesday
 */
@Repository
public interface TtsVoicePackRepository extends JpaRepository<TtsVoicePack, Long> {

}



