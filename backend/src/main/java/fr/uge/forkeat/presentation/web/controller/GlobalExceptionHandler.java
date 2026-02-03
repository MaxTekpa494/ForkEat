package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.StripEventException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(basePackages = "fr.uge.forkeat.presentation.web")
public class GlobalExceptionHandler {

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

}