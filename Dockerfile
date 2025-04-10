FROM tomcat:10-jdk17

ARG JAR_FILE
ARG USER=ramu

# Create user and group
RUN groupadd -r ${USER} && useradd -r -g ${USER} ${USER}

# Create necessary directories with proper permissions
RUN mkdir -p /usr/local/tomcat/webapps/ROOT \
    && chown -R ${USER}:${USER} /usr/local/tomcat

# Set working directory
WORKDIR /usr/local/tomcat/webapps/ROOT

# Copy the JAR file
COPY ${JAR_FILE} app.jar

# Install necessary tools
RUN apt-get update && apt-get install -y unzip

# Extract JAR contents to Tomcat webapps directory
RUN unzip app.jar -d . && rm app.jar

# Set proper ownership
RUN chown -R ${USER}:${USER} /usr/local/tomcat

# Switch to non-root user
USER ${USER}

# Expose port
EXPOSE 8080

# Command to run Tomcat
CMD ["catalina.sh", "run"]
