package hobbylist.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import hobbylist.commons.core.Messages;
import hobbylist.commons.exceptions.IllegalValueException;
import hobbylist.model.activity.Activity;
import hobbylist.model.activity.Description;
import hobbylist.model.activity.Name;
import hobbylist.model.activity.Review;
import hobbylist.model.activity.Status;
import hobbylist.model.date.Date;
import hobbylist.model.tag.Tag;

/**
 * Jackson-friendly version of {@link Activity}.
 */
class JsonAdaptedActivity {

    public static final String MISSING_FIELD_MESSAGE_FORMAT = "Activity's %s field is missing!";

    private final String name;
    private final String description;
    private final List<JsonAdaptedTag> tagged = new ArrayList<>();
    private final List<JsonAdaptedDate> date = new ArrayList<>();
    private final int rating;
    private final String status;
    private final String review;

    /**
     * Constructs a {@code JsonAdaptedActivity} with the given activity details.
     */
    @JsonCreator
    public JsonAdaptedActivity(@JsonProperty("name") String name, @JsonProperty("description") String description,
                               @JsonProperty("tagged") List<JsonAdaptedTag> tagged,
                               @JsonProperty("date") List<JsonAdaptedDate> date,
                               @JsonProperty("rating") int rating,
                               @JsonProperty("status") String status,
                               @JsonProperty("review") String review) {

        this.name = name;
        this.description = description;
        if (tagged != null) {
            this.tagged.addAll(tagged);
        }
        if (date != null) {
            this.date.addAll(date);
        }
        this.rating = rating;
        this.status = status;
        this.review = review;
    }

    /**
     * Converts a given {@code Activity} into this class for Jackson use.
     */
    public JsonAdaptedActivity(Activity source) {
        name = source.getName().fullName;
        description = source.getDescription().value;
        tagged.addAll(source.getTags().stream()
                .map(JsonAdaptedTag::new)
                .collect(Collectors.toList()));
        rating = source.getRating();
        //JsonAdaptedDate d = new JsonAdaptedDate("2003-03-03");
        //date.add(d);
        if (!source.getDate().isEmpty()) {
            date.add(new JsonAdaptedDate(source.getDate().get(0)));
        }
        status = source.getStatus().toString();
        review = source.getReview().isPresent() ? source.getReview().get().toString() : null;
    }

    /**
     * Converts this Jackson-friendly adapted activity object into the model's {@code Activity} object.
     *
     * @throws IllegalValueException if there were any data constraints violated in the adapted activity.
     */
    public Activity toModelType() throws IllegalValueException {
        final List<Tag> activityTags = new ArrayList<>();
        final List<Date> activityDate = new ArrayList<>();
        final Status modelStatus;
        for (JsonAdaptedDate date : date) {
            activityDate.add(date.toModelType());
        }
        for (JsonAdaptedTag tag : tagged) {
            activityTags.add(tag.toModelType());
        }

        if (name == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName()));
        }
        if (!Name.isValidName(name)) {
            throw new IllegalValueException(Name.MESSAGE_CONSTRAINTS);
        }
        final Name modelName = new Name(name);

        if (description == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Description.class
                    .getSimpleName()));
        }
        if (!Description.isValidDescription(description)) {
            throw new IllegalValueException(Description.MESSAGE_CONSTRAINTS);
        }
        final Description modelDescription = new Description(description);

        final Set<Tag> modelTags = new HashSet<>(activityTags);

        if (rating > 5 || rating < 0) {
            throw new IllegalValueException(Messages.MESSAGE_INVALID_RATING);
        }

        // Solution adapted from https://github.com/AY2021S1-CS2103T-F11-3/tp/pull/124/files
        if (status == null || status.equals("")) {
            modelStatus = new Status();
        } else if (Arrays.stream(Status.VALID_STATUSES).anyMatch(s -> status.equals(s))) {
            modelStatus = new Status(status);
        } else {
            throw new IllegalValueException(Status.MESSAGE_CONSTRAINT);
        }

        if (!Review.isValidReview(review)) {
            throw new IllegalValueException(Review.MESSAGE_CONSTRAINTS);
        }
        final Optional<Review> modelReview;
        if (review == null) {
            modelReview = Optional.empty();
        } else {
            modelReview = Optional.of(new Review(review));
        }

        return new Activity(modelName, modelDescription, modelTags, activityDate, rating, modelStatus, modelReview);

    }

}
