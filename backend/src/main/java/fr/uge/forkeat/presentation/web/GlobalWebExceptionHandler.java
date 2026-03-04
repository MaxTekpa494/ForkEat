package fr.uge.forkeat.presentation.web;

import fr.uge.forkeat.service.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@ControllerAdvice(basePackages = "fr.uge.forkeat.presentation.web")
public class GlobalWebExceptionHandler {

	@ExceptionHandler(RecipeNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleRecipeNotFound(RecipeNotFoundException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		model.addAttribute("pageTitle", "Recette non trouvée");
		return "error/404";
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		model.addAttribute("pageTitle", "Page non trouvée");
		return "error/404";
	}

	@ExceptionHandler(WalletNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleWalletNotFound(WalletNotFoundException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		model.addAttribute("pageTitle", "Portefeuille non trouvé");
		return "error/404";
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		model.addAttribute("pageTitle", "Requête invalide");
		return "error/400";
	}

	@ExceptionHandler(StripEventException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleStripEvent(StripEventException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		model.addAttribute("pageTitle", "Erreur de paiement");
		return "error/400";
	}

	@ExceptionHandler(DuplicateTransactionException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public String handleDuplicateTransaction(DuplicateTransactionException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		model.addAttribute("pageTitle", "Transaction en double");
		return "error/409";
	}

	@ExceptionHandler(RegisterFailureException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String HandleRegisterFailure(RegisterFailureException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		model.addAttribute("pageTitle", "Erreur lors de l'inscription");
		return "layout/register";
	}

	@ExceptionHandler(CheckProfileUpdateFailureException.class)
	public String handleUpdateFailure(CheckProfileUpdateFailureException ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return redirectTarget(request);
	}

	@ExceptionHandler(VerificationException.class)
	public String handleVerificationException(VerificationException ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return redirectTarget(request);
	}

	private String redirectTarget(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/auth/") ? "redirect:/auth/forgot-password" : "redirect:/account";
	}

	@ExceptionHandler(ImageUploadException.class)
	public String handleImageUploadException(ImageUploadException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return "redirect:/recipes/create";
	}

	@ExceptionHandler(InsufficientFundsException.class)
	public ResponseEntity<Map<String, String>> handleInsufficientFunds(InsufficientFundsException ex) {
		return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
				.body(Map.of("error", "Payment Required", "message", ex.getMessage()));
	}

	@ExceptionHandler(ModerationRagException.class)
	public String handleModeration(ModerationRagException e,
								   RedirectAttributes redirectAttributes,
								   HttpServletRequest request) {
		redirectAttributes.addFlashAttribute("moderationError", e.getMessage());
		redirectAttributes.addFlashAttribute("query", request.getParameter("query"));
		return "redirect:/recipes/smart-search";
	}

}
