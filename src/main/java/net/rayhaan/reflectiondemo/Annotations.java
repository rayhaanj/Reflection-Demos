package net.rayhaan.reflectiondemo;

import com.google.common.collect.Lists;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.List;

public class Annotations {

    public static class Meal {
        @Edible(taste = Taste.SALTY)
        public static final String starter = "Onion rings";

        @Edible(taste = Taste.UMAMI)
        public static final String mainCourse = "Beef Fried rice";

        @Edible(taste = Taste.SWEET)
        public static final String dessert = "Cheesecake";

        @Edible(taste = Taste.BITTER)
        public static final String postDessert = "Coffee";
    }

    public static void main(String... args) throws Exception {
        Field[] mealFields = Meal.class.getDeclaredFields();
        printMealTastes(new Meal());
    }

    public static void printMealTastes(Meal meal) throws IllegalAccessException {
        // Go through all the member variables of the meal's class.
        for (Field f : meal.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(Edible.class)) {
                // Get the list of compounds causing this taste out of the enum.
                List<String> others = f.getDeclaredAnnotation(Edible.class).taste().compounds;
                System.out.println(String.format("%s: %s, compounds known to give this taste: %s.",
                        f.get(meal), f.getName(), others));
            }
        }
    }

    /**
     * Represents the taste of a food, and gives examples of compounds which
     * invoke that taste.
     */
    public enum Taste {
        SWEET(Lists.newArrayList("Glucose", "Sucrose", "Aspartame", "Sucralose")),
        SOUR(Lists.newArrayList("Citric acid", "Carbonic acid", "Ascorbic acid")),
        SALTY(Lists.newArrayList("Sodium chloride")),
        BITTER(Lists.newArrayList("Phenylthiocarbamide", "Caffeine", "Quinine")),
        UMAMI(Lists.newArrayList("Glutamic acid"));

        List<String> compounds;

        Taste(List<String> compounds) {
            this.compounds = compounds;
        }

        public List<String> getCompounds() {
            return compounds;
        }
    }

    /**
     * An annotation interface allowing foods to be annotated with a taste.
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Edible {
        Taste taste();
    }
}
