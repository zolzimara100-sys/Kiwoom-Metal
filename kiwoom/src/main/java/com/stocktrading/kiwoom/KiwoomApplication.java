package com.stocktrading.kiwoom;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.stocktrading.kiwoom.dto.TokenResponse;
import com.stocktrading.kiwoom.service.KiwoomAuthService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class KiwoomApplication {

	public static void main(String[] args) {
		SpringApplication.run(KiwoomApplication.class, args);
	}

	/**
	 * 애플리케이션 시작 시 실행되는 예제 코드
	 * 실제 운영 시에는 제거하거나 주석 처리하세요
	 */
	@Bean
	public CommandLineRunner demo(KiwoomAuthService authService) {
		return (args) -> {
			log.info("=".repeat(50));
			log.info("키움증권 REST API 인증 프로그램 시작");
			log.info("=".repeat(50));
			
			// 예제 1: 토큰 발급
			log.info("\n[예제 1] 토큰 발급 테스트");
			try {
				TokenResponse token = authService.issueTokenSync();
				log.info("✓ 토큰 발급 성공!");
				log.info("  - Access Token: {}...", token.getAccessToken().substring(0, 20));
				log.info("  - Expires In: {} seconds", token.getExpiresIn());
			} catch (Exception e) {
				log.error("✗ 토큰 발급 실패: {}", e.getMessage());
				log.error("  → application.properties의 App Key와 App Secret을 확인하세요!");
			}
			
			// 예제 2: 토큰 상태 확인
			log.info("\n[예제 2] 토큰 상태 확인");
			log.info("  - 토큰 존재 여부: {}", authService.getAccessToken() != null);
			log.info("  - 토큰 만료 여부: {}", authService.isTokenExpired());
			
			// 예제 3: 유효한 토큰 가져오기 (자동 갱신)
			log.info("\n[예제 3] 유효한 토큰 조회 (자동 갱신)");
			try {
				String validToken = authService.getValidToken();
				log.info("✓ 유효한 토큰: {}...", validToken.substring(0, 20));
			} catch (Exception e) {
				log.error("✗ 토큰 조회 실패: {}", e.getMessage());
			}
			
			log.info("\n" + "=".repeat(50));
			log.info("애플리케이션이 실행 중입니다.");
			log.info("REST API 테스트: http://localhost:8080/api/kiwoom/health");
			log.info("=".repeat(50) + "\n");
		};
	}

}
