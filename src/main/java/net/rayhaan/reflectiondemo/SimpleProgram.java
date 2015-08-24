package net.rayhaan.reflectiondemo;

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple program which has a dynamically created menu.
 */
public class SimpleProgram {

    public static void main(String... args) {
        Program p = new Program();
        Menu m = new Menu(p);
        m.run();
    }

    public static class Program implements MenuApplet {
        public String applicationName() {
            return "Simple program";
        }

        public String about() {
            return "A simple demonstration program.";
        }

        @MenuOpt("Bark")
        public void bark() {
            System.out.println("Woof");
        }

        @MenuOpt("Print name variable number of times")
        public void multiName(@UserAccessibleParameter(paramName = "name") String name,
                              @UserAccessibleParameter(paramName = "repetitions")int reps) {
            for (int i = 0; i < reps; i++) {
                System.out.println(name);
            }
        }

        @MenuOpt("Rawr!")
        public void rawr(@UserAccessibleParameter(paramName = "name") String name) {
            System.out.println("Rawr, " + name);
        }

    }

    private interface MenuApplet {
        String applicationName();
        String about();
    }

    /**
     * Indicates that the annotated methods should be menu items.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)  // Specifies that this annotation is used on methods.
    public @interface MenuOpt {
        String value();  // Contains the menu text to be printed.
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface UserAccessibleParameter {
        String paramName();  // Name to display when prompting the user for a value.
    }

    /**
     * An annotation based menu implementation.
     */
    public static class Menu {

        /**
         * An item to be displayed in the menu.
         */
        private class MenuItem {
            public String name;
            public Method method;

            public MenuItem(String name, Method method) {
                this.name = name;
                this.method = method;
            }
        }

        private HashMap<Integer, MenuItem> menuItems;
        private MenuApplet instance;

        public Menu (MenuApplet app) {
            this.instance = app;
            this.populateMenu(app);
        }

        /**
         * Get all the decorated methods out, check their types are primitive, and add them to the
         * list of methods.
         */
        private void populateMenu(MenuApplet app) {
            this.menuItems = new HashMap<Integer, MenuItem>();

            int itemCount = 0;
            Class<?> clazz = app.getClass();
            MethodLoop: for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(MenuOpt.class)) {
                    // Check that the method only has primitive arguments, or no arguments.
                    if (m.getParameterCount() > 0) {
                        Parameter[] parameters = m.getParameters();
                        for (Parameter p : parameters) {
                            Class<?> type = p.getType();
                            if (type != String.class && type != int.class && type!= double.class
                                    && type != float.class && type != long.class
                                    && type != char.class && type != short.class) {
                                System.err.println("Method " + m.getName() + " has been skipped as"
                                    + " a menu option because it has a non primitive parameter");
                                continue MethodLoop;
                            }
                        }
                    }
                    itemCount++;
                    String menuDescription = m.getAnnotation(MenuOpt.class).value();
                    // Add this option to the menu.
                    menuItems.put(itemCount, new MenuItem(menuDescription, m));
                }
            }
        }

    private boolean invoke(int index) {
        MenuItem mItem = menuItems.get(index);
        if (mItem == null) {
            System.out.println("No such option " + index);
            return false;
        }
        Method m = mItem.method;

        List<Object> parameters = Lists.newArrayList();
        // If there are parameters to input, then ask the user for them.
        if (m.getParameterCount() > 0) {
            Parameter[] formalParameters = m.getParameters();
            for (Parameter p : formalParameters) {
                // Ask the user for the value of the parameter and store it.
                if (p.isAnnotationPresent(UserAccessibleParameter.class)) {
                    System.out.println("Please enter a " + p.getType().getName()
                            + " value for the parameter: "
                            + p.getAnnotation(UserAccessibleParameter.class).paramName());
                } else {
                    System.out.println("Please enter a " + p.getType().getName()
                            + " value for the parameter: " + p.getName());
                }

                String paramVal = readLine();
                if (p.getType().equals(String.class)) {
                    parameters.add(paramVal);
                } else if (p.getType().equals(int.class)) {
                    parameters.add(Integer.parseInt(paramVal));
                } else if (p.getType().equals(double.class)) {
                    parameters.add(Double.parseDouble(paramVal));
                } else if (p.getType().equals(float.class)) {
                    parameters.add(Float.parseFloat(paramVal));
                } else if (p.getType().equals(long.class)) {
                    parameters.add(Long.parseLong(paramVal));
                } else if (p.getType().equals(char.class)) {
                    parameters.add(paramVal.charAt(0));
                } else if (p.getType().equals(short.class)) {
                    parameters.add(Short.parseShort(paramVal));
                } else {
                    throw new IllegalStateException("A non primitive parameter value was found!");
                }
            }
        }

        try {
            m.invoke(this.instance, parameters.toArray());
        } catch (IllegalAccessException e) {
            System.out.println("There was an error running the menu item");
            return false;
        } catch (InvocationTargetException e) {
            System.out.println("The called method threw an exception: " + e);
            return false;
        }
        return true;
    }

        public String generatePrompt() {
            StringBuilder sb = new StringBuilder();
            sb.append("The following options are available: \n");
            for (Map.Entry<Integer, MenuItem> entry : this.menuItems.entrySet()) {
                sb.append("\t");
                sb.append("(");
                sb.append(entry.getKey());
                sb.append(") ");
                sb.append(entry.getValue().name);
                sb.append("\n");
            }
            sb.append("Please select an option: ");
            return sb.toString();
        }

        public void run() {
            System.out.println(instance.applicationName() + "\n" + instance.about() + "\n\n");

            String options = generatePrompt();
            while (true) {
                System.out.println(options);
                String input = readLine();
                if (input.equals("q") || input.equals("quit")) {
                    break;
                }
                int choice = 0;
                try {
                    choice = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number or 'q' to quit");
                    continue;
                }
                invoke(choice);
            }
        }
    }

    private static String readLine() {
		String s = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			s = br.readLine();
		} catch (Exception e) {
			System.out.println("Error reading input!");
		}
		return s;
	}

}
