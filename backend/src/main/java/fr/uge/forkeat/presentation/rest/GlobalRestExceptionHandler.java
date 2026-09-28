package fr.uge.forkeat.presentation.rest;

import fr.uge.forkeat.presentation.response.*;
import fr.uge.forkeat.service.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = {"fr.uge.forkeat.presentation.rest", "fr.uge.forkeat.presentation.external"})
public class GlobalRestExceptionHandler {

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<HttpResponse<Void>> handleForbiddenOperation(ForbiddenOperationException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ForbiddenResponse(e.getMessage()));
    }

    @ExceptionHandler(RecipeOwnershipException.class)
    public ResponseEntity<HttpResponse<Void>> handleRecipeOwnership(RecipeOwnershipException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ForbiddenResponse(e.getMessage()));
    }

    @ExceptionHandler(RecipeNotFoundException.class)
    public ResponseEntity<HttpResponse<Void>> handleRecipeNotFound(RecipeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new NotFoundResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HttpResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BadRequestResponse(e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<HttpResponse<Void>> handleResourceNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new NotFoundResponse(e.getMessage()));
    }

    @ExceptionHandler(StripEventException.class)
    public ResponseEntity<HttpResponse<Void>> handleSignatureVerification(StripEventException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BadRequestResponse(e.getMessage()));
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<HttpResponse<Void>> handleDuplicateTransaction(DuplicateTransactionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ConflictResponse(e.getMessage()));
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<HttpResponse<Void>> handleWalletNotFound(WalletNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new NotFoundResponse(e.getMessage()));
    }

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<HttpResponse<Void>> handleVerificationException(VerificationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BadRequestResponse(e.getMessage()));
    }

    @ExceptionHandler(RegisterFailureException.class)
    public ResponseEntity<HttpResponse<Void>> handleRegisterFailure(RegisterFailureException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BadRequestResponse(e.getMessage()));
    }

    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<HttpResponse<Void>> handleImageUploadException(ImageUploadException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BadRequestResponse(e.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<HttpResponse<Void>> insufficientFundsException(InsufficientFundsException e) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(new PaymentRequiredResponse(e.getMessage()));
    }

    @ExceptionHandler(CheckProfileUpdateFailureException.class)
    public ResponseEntity<HttpResponse<Void>> handleCheckProfileUpdateFailure(CheckProfileUpdateFailureException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BadRequestResponse(e.getMessage()));
    }

    @ExceptionHandler(ModerationRagException.class)
    public ResponseEntity<HttpResponse<Void>> handleModeration(ModerationRagException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BadRequestResponse(e.getMessage()));
    }

    @ExceptionHandler(AuthenticationTokenException.class)
    public ResponseEntity<HttpResponse<Void>> authenticationException(AuthenticationTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new UnauthorizedResponse(e.getMessage()));
    }

    @ExceptionHandler(RecipeAlreadyReportedException.class)
    public ResponseEntity<HttpResponse<Void>> handleRecipeAlreadyReported(RecipeAlreadyReportedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ConflictResponse(e.getMessage()));
    }

    @ExceptionHandler(UserAlreadyReportedException.class)
    public ResponseEntity<HttpResponse<Void>> handleUserAlreadyReported(UserAlreadyReportedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ConflictResponse(e.getMessage()));
    }

    @ExceptionHandler(PromotionNotFoundException.class)
    public ResponseEntity<HttpResponse<Void>> handlePromotionNotFound(PromotionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new NotFoundResponse(e.getMessage()));
    }

    @ExceptionHandler(PromotionOverlapException.class)
    public ResponseEntity<HttpResponse<Void>> handlePromotionOverlap(PromotionOverlapException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ConflictResponse(e.getMessage()));
    }

    @ExceptionHandler(PromotionModificationForbiddenException.class)
    public ResponseEntity<HttpResponse<Void>> handlePromotionModificationForbidden(PromotionModificationForbiddenException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new UnprocessableContentResponse(e.getMessage()));
    }

    @ExceptionHandler(PromotionNotProfitableException.class)
    public ResponseEntity<HttpResponse<Void>> handlePromotionNotProfitable(PromotionNotProfitableException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new UnprocessableContentResponse(e.getMessage()));
    }
}
